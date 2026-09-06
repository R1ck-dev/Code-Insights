# Funcoes compartilhadas pelos scripts de scripts/. Nao roda nada sozinho: e para ser
# dot-sourced (". $PSScriptRoot\comum.ps1").
#
# Mantenha em ASCII puro: o Windows PowerShell 5.1 le .ps1 como ANSI quando nao ha BOM, e um
# caractere acentuado gravado em UTF-8 vira erro de parser.

function Get-RaizDoProjeto {
    return (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
}

<#
.SYNOPSIS
Carrega o .env da raiz nas variaveis de ambiente do processo atual.
.DESCRIPTION
O projeto nao tem biblioteca dotenv no pom, entao o Spring Boot nao le o .env sozinho. No VS Code
quem faz isso e o "envFile" do launch.json; no terminal, e esta funcao.
#>
function Import-DotEnv {
    $arquivo = Join-Path (Get-RaizDoProjeto) '.env'
    if (-not (Test-Path $arquivo)) {
        throw "Arquivo .env nao encontrado na raiz. Ele nao e versionado: copie de outra maquina ou recrie (JWT_SECRET com no minimo 32 caracteres)."
    }

    Get-Content $arquivo | ForEach-Object {
        $linha = $_.Trim()
        if ($linha -eq '' -or $linha.StartsWith('#')) { return }
        $sep = $linha.IndexOf('=')
        if ($sep -lt 1) { return }
        $nome = $linha.Substring(0, $sep).Trim()
        $valor = $linha.Substring($sep + 1).Trim().Trim('"').Trim("'")
        Set-Item -Path "env:$nome" -Value $valor
    }
}

function Get-ProcessoNaPorta {
    param([Parameter(Mandatory)][int] $Porta)

    $conexao = Get-NetTCPConnection -LocalPort $Porta -State Listen -ErrorAction SilentlyContinue
    if (-not $conexao) { return $null }
    return Get-Process -Id $conexao[0].OwningProcess -ErrorAction SilentlyContinue
}

function Test-PortaResponde {
    param([Parameter(Mandatory)][int] $Porta)

    try {
        $cliente = New-Object System.Net.Sockets.TcpClient
        $cliente.Connect('localhost', $Porta)
        $cliente.Close()
        return $true
    } catch {
        return $false
    }
}

<#
.SYNOPSIS
Diz se a URL responde 200.
.DESCRIPTION
Porta ocupada nao e a mesma coisa que servico no ar: um processo zumbi segura o socket sem atender
nada, e um Spring a meio boot ja abriu a porta antes de estar pronto. Quem decide as duas coisas
e a resposta HTTP, nao o socket.
#>
function Test-HttpOk {
    param(
        [Parameter(Mandatory)][string] $Url,
        [int] $LimiteSegundos = 3
    )

    try {
        $resposta = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec $LimiteSegundos -ErrorAction Stop
        return $resposta.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Wait-Http {
    param(
        [Parameter(Mandatory)][string] $Url,
        [Parameter(Mandatory)][string] $Nome,
        [int] $LimiteSegundos = 180
    )

    Write-Host "  aguardando $Nome responder..." -NoNewline
    for ($i = 0; $i -lt $LimiteSegundos; $i++) {
        if (Test-HttpOk -Url $Url) {
            Write-Host " no ar" -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 1
    }
    Write-Host " TEMPO ESGOTADO" -ForegroundColor Red
    return $false
}

<#
.SYNOPSIS
Encerra quem estiver escutando a porta.
.DESCRIPTION
Mata SEMPRE pelo dono do socket, nunca por nome de processo. Um "Stop-Process -Name java" levaria
junto o language server do VS Code (extensao redhat.java), que tambem roda como java e nao tem nada
a ver com o backend.
#>
function Stop-ProcessoNaPorta {
    param(
        [Parameter(Mandatory)][int] $Porta,
        [Parameter(Mandatory)][string] $Nome
    )

    $processo = Get-ProcessoNaPorta -Porta $Porta
    if (-not $processo) {
        Write-Host "  $Nome (porta $Porta): ja estava parado" -ForegroundColor DarkGray
        return $false
    }

    Stop-Process -Id $processo.Id -Force
    Write-Host "  $Nome (porta $Porta): $($processo.ProcessName) PID $($processo.Id) encerrado" -ForegroundColor Green
    return $true
}

function Wait-PortaLivre {
    param(
        [Parameter(Mandatory)][int] $Porta,
        [int] $LimiteSegundos = 20
    )

    for ($i = 0; $i -lt $LimiteSegundos; $i++) {
        if (-not (Get-ProcessoNaPorta -Porta $Porta)) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

<#
.SYNOPSIS
Deixa a porta pronta para uso. Devolve 'livre' (pode subir) ou 'reaproveitar' (ja ha um servico bom).
.DESCRIPTION
O caso que motivou esta funcao: fechar a janela do run-local mata o Maven, mas o java que ele forkou
sobrevive segurando a 8080. Na proxima subida a porta parece ocupada por um backend que, na verdade,
nao atende mais. O mesmo vale para o node do Vite na 5173.

Por isso a decisao nao e "tem alguem na porta?", e sim "o que esta na porta ainda serve?".
#>
function Initialize-Porta {
    param(
        [Parameter(Mandatory)][int] $Porta,
        [Parameter(Mandatory)][string] $Nome,
        [Parameter(Mandatory)][string] $UrlSaude,
        [switch] $Forcar
    )

    $processo = Get-ProcessoNaPorta -Porta $Porta
    if (-not $processo) {
        return 'livre'
    }

    $saudavel = Test-HttpOk -Url $UrlSaude

    if ($saudavel -and -not $Forcar) {
        Write-Host "$Nome ja esta no ar e respondendo (PID $($processo.Id)) - reaproveitando." -ForegroundColor Yellow
        return 'reaproveitar'
    }

    $motivo = if ($Forcar) { '-Reiniciar pedido' } else { "nao responde em $UrlSaude" }
    Write-Host "Porta $Porta ocupada por $($processo.ProcessName) PID $($processo.Id) ($motivo)." -ForegroundColor Yellow
    Stop-ProcessoNaPorta -Porta $Porta -Nome $Nome | Out-Null

    if (-not (Wait-PortaLivre -Porta $Porta)) {
        throw "A porta $Porta continua ocupada apos encerrar o processo. Verifique com: Get-Process -Id (Get-NetTCPConnection -LocalPort $Porta -State Listen).OwningProcess"
    }
    return 'livre'
}

function Wait-Porta {
    param(
        [Parameter(Mandatory)][int] $Porta,
        [Parameter(Mandatory)][string] $Nome,
        [int] $LimiteSegundos = 120
    )

    Write-Host "  aguardando $Nome na porta $Porta..." -NoNewline
    for ($i = 0; $i -lt $LimiteSegundos; $i++) {
        if (Test-PortaResponde -Porta $Porta) {
            Write-Host " no ar" -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 1
    }
    Write-Host " TEMPO ESGOTADO" -ForegroundColor Red
    return $false
}

<#
.SYNOPSIS
Executa o docker e falha apenas quando ele realmente falha.
.DESCRIPTION
Resolve dois problemas do PowerShell 5.1 com executaveis nativos.

1. O docker escreve o progresso ("Container ... Running") no STDERR mesmo dando tudo certo, e
   stderr de nativo vira ErrorRecord assim que ha redirecionamento. Com $ErrorActionPreference =
   'Stop' o script abortaria com o comando tendo funcionado. A preferencia cai so aqui dentro e
   quem decide sucesso e o codigo de saida.

2. Os argumentos chegam como UM array ($Argumentos e posicional, nao ValueFromRemainingArguments).
   Passados soltos, o PowerShell le "-d" como NOME DE PARAMETRO; nada casa, o parametro Mandatory
   fica vazio e o PowerShell abre um prompt pedindo o valor. Sem ninguem para responder no stdin,
   o script trava para sempre sem mensagem nenhuma.
       ERRADO:  Invoke-Docker compose up -d
       CERTO:   Invoke-Docker @('compose', 'up', '-d')

A saida e ESCOADA linha a linha, nao acumulada numa variavel: na primeira subida de uma maquina
o docker baixa as imagens, e capturar deixaria a tela muda por minutos. O `2>&1 | ForEach-Object`
existe para imprimir as linhas de stderr como texto - sem ele, elas apareceriam como ErrorRecord,
com o bloco CategoryInfo/FullyQualifiedErrorId junto, sempre que houvesse redirecionamento.
#>
function Invoke-Docker {
    param([Parameter(Mandatory, Position = 0)][string[]] $Argumentos)

    $anterior = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & docker @Argumentos 2>&1 | ForEach-Object {
            Write-Host "  $_" -ForegroundColor DarkGray
        }
        if ($LASTEXITCODE -ne 0) {
            throw "docker $($Argumentos -join ' ') falhou (codigo $LASTEXITCODE)."
        }
    } finally {
        $ErrorActionPreference = $anterior
    }
}

function Test-DockerNoAr {
    $anterior = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & docker info --format '{{.ServerVersion}}' 2>&1 | Out-Null
        return $LASTEXITCODE -eq 0
    } finally {
        $ErrorActionPreference = $anterior
    }
}

<#
.SYNOPSIS
Garante o daemon do Docker no ar, iniciando o Docker Desktop se preciso.
#>
function Start-DockerSePreciso {
    if (Test-DockerNoAr) { return $true }

    $executavel = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
    if (-not (Test-Path $executavel)) {
        Write-Host "Docker Desktop nao encontrado em $executavel" -ForegroundColor Red
        return $false
    }

    Write-Host "Docker parado. Iniciando o Docker Desktop..." -ForegroundColor Yellow
    Start-Process $executavel

    Write-Host "  aguardando o daemon..." -NoNewline
    for ($i = 0; $i -lt 120; $i++) {
        if (Test-DockerNoAr) {
            Write-Host " no ar" -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 2
    }
    Write-Host " TEMPO ESGOTADO" -ForegroundColor Red
    Write-Host "Se o daemon nunca sobe, confira se WSL2 esta habilitado: 'wsl --status'." -ForegroundColor Yellow
    return $false
}
