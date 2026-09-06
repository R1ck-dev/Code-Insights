# Derruba o ambiente local: backend, frontend e containers.
#
# Uso:
#   .\scripts\parar.ps1              para os processos e os containers (PRESERVA o banco)
#   .\scripts\parar.ps1 -ApagarBanco derruba tambem o volume do Postgres
#
# -ApagarBanco e destrutivo: leva junto a coorte semeada e qualquer conta criada a mao. Depois
# dele, o caminho de volta e .\scripts\semear.ps1.
#
# Mantenha em ASCII puro (ver comum.ps1).

param(
    [switch] $ApagarBanco
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\comum.ps1"

$raiz = Get-RaizDoProjeto

Write-Host "Parando processos locais..." -ForegroundColor Cyan
Stop-ProcessoNaPorta -Porta 8080 -Nome 'backend' | Out-Null
Stop-ProcessoNaPorta -Porta 5173 -Nome 'frontend' | Out-Null

if (-not (Test-DockerNoAr)) {
    Write-Host "Docker ja esta parado - nada a derrubar." -ForegroundColor DarkGray
    return
}

if ($ApagarBanco) {
    Write-Host ""
    Write-Host "ATENCAO: isto apaga o banco local inteiro (coorte semeada inclusive)." -ForegroundColor Red
    $resposta = Read-Host "Digite APAGAR para confirmar"
    if ($resposta -cne 'APAGAR') {
        Write-Host "Cancelado. Os containers continuam no ar." -ForegroundColor Yellow
        return
    }
    Invoke-Docker @('compose', '--project-directory', $raiz, 'down', '-v')
    Write-Host "Containers e volume do Postgres removidos." -ForegroundColor Green
    Write-Host "Para repovoar:  .\scripts\semear.ps1" -ForegroundColor DarkGray
} else {
    Invoke-Docker @('compose', '--project-directory', $raiz, 'down')
    Write-Host "Containers parados. O banco foi preservado." -ForegroundColor Green
}
