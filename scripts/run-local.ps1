# Sobe o backend lendo as variaveis do .env da raiz.
#
# Existe porque o projeto nao tem biblioteca dotenv no pom: o Spring Boot nao le o .env
# sozinho. A config de launch do VS Code (.vscode/launch.json) resolve isso via "envFile",
# mas no terminal o boot falharia com JWT_SECRET ausente. Este script cobre o terminal.
#
# Mantenha este arquivo em ASCII puro: o Windows PowerShell 5.1 le .ps1 como ANSI quando
# nao ha BOM, e um caractere acentuado gravado em UTF-8 vira erro de parser.
#
# Uso:  .\run-local.ps1

$ErrorActionPreference = 'Stop'
$raiz = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$arquivoEnv = Join-Path $raiz '.env'

if (-not (Test-Path $arquivoEnv)) {
    throw "Arquivo .env nao encontrado em $raiz. Ele nao e versionado: copie de outra maquina ou recrie (JWT_SECRET com no minimo 32 caracteres)."
}

Get-Content $arquivoEnv | ForEach-Object {
    $linha = $_.Trim()
    # Ignora linha vazia e comentario (as variaveis desativadas do .env sao comentarios).
    if ($linha -eq '' -or $linha.StartsWith('#')) { return }
    $sep = $linha.IndexOf('=')
    if ($sep -lt 1) { return }
    $nome = $linha.Substring(0, $sep).Trim()
    $valor = $linha.Substring($sep + 1).Trim().Trim('"').Trim("'")
    Set-Item -Path "env:$nome" -Value $valor
    Write-Host "  $nome carregado" -ForegroundColor DarkGray
}

if (-not $env:JWT_SECRET) {
    throw "JWT_SECRET nao foi definido pelo .env, entao o boot falharia."
}

Write-Host "Subindo o backend em http://localhost:8080 (perfil local)..." -ForegroundColor Cyan
& (Join-Path $raiz 'mvnw.cmd') spring-boot:run
