# Sincroniza o projeto depois de mexer nele em outra maquina.
#
# Uso:
#   .\scripts\atualizar.ps1             git pull + dependencias + compilacao
#   .\scripts\atualizar.ps1 -ComTestes  roda tambem a suite completa do backend
#   .\scripts\atualizar.ps1 -SemPull    so refaz dependencias e compila
#
# Mantenha em ASCII puro (ver comum.ps1).

param(
    [switch] $ComTestes,
    [switch] $SemPull
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\comum.ps1"

$raiz = Get-RaizDoProjeto
Set-Location $raiz

function Assert-UltimoComandoOk {
    param([Parameter(Mandatory)][string] $Passo)

    if ($LASTEXITCODE -ne 0) {
        throw "$Passo falhou (codigo $LASTEXITCODE). Nada depois disso foi executado."
    }
}

if (-not $SemPull) {
    Write-Host "== git pull ==" -ForegroundColor Cyan
    $pendentes = git status --porcelain
    if ($pendentes) {
        Write-Host "Ha alteracoes nao commitadas:" -ForegroundColor Yellow
        $pendentes | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
        Write-Host "O pull segue mesmo assim; se der conflito, resolva antes de continuar." -ForegroundColor Yellow
    }
    git pull --ff-only
    Assert-UltimoComandoOk 'git pull'
}

Write-Host ""
Write-Host "== dependencias do frontend ==" -ForegroundColor Cyan
Set-Location (Join-Path $raiz 'frontend')
# npm ci (e nao install) para reproduzir exatamente o package-lock.json: e o mesmo estado da
# outra maquina, e nao "o que o npm resolveria hoje".
npm ci
Assert-UltimoComandoOk 'npm ci'

Write-Host ""
Write-Host "== typecheck e build do frontend ==" -ForegroundColor Cyan
npm run build
Assert-UltimoComandoOk 'build do frontend'

Set-Location $raiz
Write-Host ""
if ($ComTestes) {
    Write-Host "== backend: compilacao e testes ==" -ForegroundColor Cyan
    .\mvnw.cmd -B clean test
    Assert-UltimoComandoOk 'testes do backend'
} else {
    Write-Host "== backend: compilacao ==" -ForegroundColor Cyan
    .\mvnw.cmd -B -q clean test-compile
    Assert-UltimoComandoOk 'compilacao do backend'
    Write-Host "Testes pulados. Use -ComTestes para roda-los." -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "Projeto atualizado." -ForegroundColor Green
Write-Host "Se houve migration nova, ela roda sozinha no proximo boot do backend." -ForegroundColor DarkGray
Write-Host "Subir tudo:  .\scripts\dev.ps1" -ForegroundColor DarkGray
