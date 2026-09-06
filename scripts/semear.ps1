# Popula a base local com a coorte ficticia usada para demonstracao e prints.
#
# Tres passos, nesta ordem obrigatoria:
#   1. o seed entra PELA API, para cada resolucao passar pelo motor de analise de verdade;
#   2. espera a analise assincrona terminar;
#   3. o SQL espalha as datas ao longo de 6 meses.
# Inverter 2 e 3 deixaria parte das metricas com a data de hoje.
#
# Uso:
#   .\scripts\semear.ps1            semeia por cima do que existe
#   .\scripts\semear.ps1 -Zerar     apaga desafios/resolucoes/snippets/consentimentos antes
#
# Mantenha em ASCII puro (ver comum.ps1).

param(
    [switch] $Zerar
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\comum.ps1"

$raiz = Get-RaizDoProjeto
$CONTAINER = 'codeinsights-db'

function Invoke-Psql {
    param([Parameter(Mandatory)][string] $Sql)

    # $ErrorActionPreference cai aqui dentro pelo mesmo motivo do Invoke-Docker em comum.ps1: com
    # 'Stop', qualquer linha que o psql mandasse ao stderr abortaria o script antes de olharmos o
    # codigo de saida, que e quem de fato diz se a consulta passou.
    $anterior = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $saida = $Sql | docker exec -i $CONTAINER psql -U $env:DB_USER -d codeinsights -t -A -v ON_ERROR_STOP=1 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "psql falhou: $saida"
        }
        return ($saida | Out-String).Trim()
    } finally {
        $ErrorActionPreference = $anterior
    }
}

Import-DotEnv
if (-not $env:DB_USER) { $env:DB_USER = 'codeinsights_admin' }
if (-not $env:ADMIN_PASSWORD) {
    throw "ADMIN_PASSWORD nao esta no .env. Sem a conta admin o seed nao consegue ativar as contas."
}

if (-not (Test-PortaResponde -Porta 8080)) {
    throw "Backend nao esta no ar em :8080. Rode .\scripts\dev.ps1 antes."
}

if ($Zerar) {
    Write-Host "Apagando o conteudo anterior..." -ForegroundColor Yellow
    # As contas de aluno vao junto: as tabelas de conteudo tem ON DELETE CASCADE a partir de
    # usuarios, e deixar as contas sem conteudo produziria um diretorio de perfis vazios.
    Invoke-Psql @"
DELETE FROM usuarios
WHERE email LIKE '%@codeinsights.dev';
"@ | Out-Null
    Write-Host "  contas @codeinsights.dev e todo o conteudo delas removidos" -ForegroundColor Green
}

Write-Host ""
Write-Host "== 1/3 semeando pela API ==" -ForegroundColor Cyan
Set-Location $raiz
node scripts/seed/seed.mjs
if ($LASTEXITCODE -ne 0) {
    throw "O seed falhou. Nada de datas foi reescrito, entao da para corrigir e rodar de novo."
}

Write-Host ""
Write-Host "== 2/3 aguardando a analise assincrona ==" -ForegroundColor Cyan
$pendentes = 'inicial'
for ($i = 0; $i -lt 180; $i++) {
    $pendentes = Invoke-Psql 'SELECT COUNT(*) FROM resolucoes WHERE analisada = false;'
    if ($pendentes -eq '0') { break }
    Write-Host "  faltam $pendentes resolucoes..." -ForegroundColor DarkGray
    Start-Sleep -Seconds 2
}
if ($pendentes -ne '0') {
    throw "Ainda ha $pendentes resolucoes sem analise. Nao vou reescrever as datas com o motor a meio caminho."
}
Write-Host "  todas analisadas" -ForegroundColor Green

Write-Host ""
Write-Host "== 3/3 espalhando as datas em 6 meses ==" -ForegroundColor Cyan
$sql = Get-Content (Join-Path $PSScriptRoot 'seed\espalhar-datas.sql') -Raw
Invoke-Psql $sql | Out-Null

$resumo = Invoke-Psql @"
SELECT (SELECT COUNT(*) FROM usuarios WHERE email LIKE '%@codeinsights.dev')
    || ' contas | ' || (SELECT COUNT(*) FROM desafios) || ' desafios | '
    || (SELECT COUNT(*) FROM resolucoes) || ' resolucoes | '
    || (SELECT COUNT(*) FROM resultados_metrica) || ' metricas | '
    || (SELECT COUNT(*) FROM snippets) || ' snippets';
"@

Write-Host ""
Write-Host "Base semeada: $resumo" -ForegroundColor Green
Write-Host ""
Write-Host "Entrar como aluno:        henriquemarangoni@codeinsights.dev / Senha@2026"
Write-Host "Entrar como pesquisadora: pesquisa@codeinsights.dev / Senha@2026"
$emailAdmin = if ($env:ADMIN_EMAIL) { $env:ADMIN_EMAIL } else { 'admin@codeinsights.app' }
Write-Host "Entrar como admin:        $emailAdmin / (ADMIN_PASSWORD do .env)"
