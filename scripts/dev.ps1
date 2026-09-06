# Sobe o ambiente local inteiro: infra no Docker, backend e frontend.
#
# Back e front abrem em JANELAS SEPARADAS de proposito. Rodar os dois nesta janela misturaria os
# dois logs, e o log do Spring e onde aparece o erro que voce vai querer ler.
#
# Porta ocupada nao trava a subida: se ja houver algo na 8080 (ou na 5173) que NAO responde, o
# script encerra esse processo e sobe um novo. E o caso do java orfao que o Maven deixa para tras
# quando a janela do run-local e fechada: ele segura a porta sem atender ninguem.
#
# Uso:
#   .\scripts\dev.ps1              sobe tudo (reaproveita o que ja estiver saudavel)
#   .\scripts\dev.ps1 -Reiniciar   derruba back e front mesmo saudaveis e sobe de novo
#   .\scripts\dev.ps1 -SemFront    so infra + backend
#
# Mantenha em ASCII puro (ver comum.ps1).

param(
    [switch] $SemFront,
    [switch] $Reiniciar
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\comum.ps1"

$raiz = Get-RaizDoProjeto

# /health e publico no SecurityConfig - da para checar sem token.
$URL_SAUDE_BACKEND = 'http://localhost:8080/health'
$URL_SAUDE_FRONT = 'http://localhost:5173/'

Write-Host "== CodeInsights: ambiente local ==" -ForegroundColor Cyan

if (-not (Start-DockerSePreciso)) {
    throw "Docker indisponivel. Sem ele nao ha Postgres, e o backend nao sobe."
}

Write-Host "Subindo Postgres e MailHog..." -ForegroundColor Cyan
Invoke-Docker @('compose', '--project-directory', $raiz, 'up', '-d')
if (-not (Wait-Porta -Porta 5434 -Nome 'Postgres')) {
    throw "Postgres nao respondeu. Veja 'docker compose logs postgres'."
}

if ((Initialize-Porta -Porta 8080 -Nome 'backend' -UrlSaude $URL_SAUDE_BACKEND -Forcar:$Reiniciar) -eq 'livre') {
    Write-Host "Abrindo o backend em nova janela..." -ForegroundColor Cyan
    # As aspas em volta do caminho NAO sao decorativas: o Start-Process junta o -ArgumentList com
    # espacos e sem citar nada, e o caminho deste projeto tem espaco ("Iniciacao Cientifica"). Sem
    # elas o powershell recebe o caminho partido ao meio, a janela morre na hora e o dev.ps1 fica
    # esperando um backend que nunca foi iniciado.
    Start-Process powershell -ArgumentList @(
        '-NoExit', '-NoProfile', '-ExecutionPolicy', 'Bypass',
        '-File', "`"$PSScriptRoot\run-local.ps1`""
    )
    # Espera o /health, e nao a porta: o Tomcat abre a 8080 antes de o Flyway e o contexto Spring
    # terminarem, entao "porta aberta" chegaria a dizer que esta pronto quando ainda nao esta.
    if (-not (Wait-Http -Url $URL_SAUDE_BACKEND -Nome 'backend' -LimiteSegundos 180)) {
        throw "Backend nao subiu. Leia o erro na janela que abriu."
    }
}

if (-not $SemFront) {
    if ((Initialize-Porta -Porta 5173 -Nome 'frontend' -UrlSaude $URL_SAUDE_FRONT -Forcar:$Reiniciar) -eq 'livre') {
        Write-Host "Abrindo o frontend em nova janela..." -ForegroundColor Cyan
        # Aspas pelo mesmo motivo do backend: sem elas o -Command chega partido no espaco do caminho.
        Start-Process powershell -ArgumentList @(
            '-NoExit', '-NoProfile', '-ExecutionPolicy', 'Bypass',
            '-Command', "`"Set-Location '$raiz\frontend'; npm run dev`""
        )
        Wait-Http -Url $URL_SAUDE_FRONT -Nome 'frontend' -LimiteSegundos 90 | Out-Null
    }
}

Write-Host ""
Write-Host "Tudo no ar:" -ForegroundColor Green
Write-Host "  App        http://localhost:5173"
Write-Host "  API        http://localhost:8080"
Write-Host "  Swagger    http://localhost:8080/swagger-ui.html"
Write-Host "  E-mails    http://localhost:8026"
Write-Host ""
Write-Host "Para derrubar tudo:  .\scripts\parar.ps1" -ForegroundColor DarkGray
