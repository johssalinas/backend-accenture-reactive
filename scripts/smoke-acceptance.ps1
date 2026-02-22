param(
    [switch]$KeepEnvironment
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$job = $null
$acceptanceExitCode = 1
$dbPort = '55433'

try {
    $existingListener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
    if ($existingListener) {
        throw 'Ya existe un proceso escuchando en localhost:8080. Deténlo antes de ejecutar el smoke test.'
    }

    Write-Host '==> Levantando base de pruebas...'
    docker compose --env-file .env.test -f deployment/docker-compose.test.yml up -d | Out-Host

    Write-Host '==> Iniciando API en background...'
    $job = Start-Job -Name appservice-smoke -ScriptBlock {
        param($dbPort)
        Set-Location 'C:\Users\braya\Documents\backend-accenture-reactive'
        .\gradlew :app-service:bootRun --args="--adapters.r2dbc.host=localhost --adapters.r2dbc.port=$dbPort --adapters.r2dbc.database=accenture_db_test --adapters.r2dbc.schema=public --adapters.r2dbc.username=postgres --adapters.r2dbc.password=postgres --spring.flyway.url=jdbc:postgresql://localhost:$dbPort/accenture_db_test --spring.flyway.user=postgres --spring.flyway.password=postgres"
    } -ArgumentList $dbPort

    Write-Host '==> Esperando que la API esté disponible...'
    $maxAttempts = 90
    $apiUp = $false

    for ($i = 1; $i -le $maxAttempts; $i++) {
        Start-Sleep -Seconds 2
        try {
            $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/franquicias' -Method Get -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                if ($job.State -ne 'Running') {
                    throw 'La API respondió en :8080 pero el proceso de bootRun no está en ejecución. Abortando para evitar falso positivo.'
                }
                $apiUp = $true
                break
            }
        }
        catch {
            if ($job -and $job.State -eq 'Failed') {
                Write-Host '==> La API falló durante el arranque:'
                Receive-Job -Job $job -Keep | Out-Host
                break
            }
        }
    }

    if (-not $apiUp) {
        throw 'La API no estuvo disponible en http://localhost:8080 dentro del tiempo esperado.'
    }

    Write-Host '==> Ejecutando acceptance tests (Karate)...'
    Set-Location "$root\deployment\acceptanceTest"
    .\gradlew test --no-daemon
    $acceptanceExitCode = $LASTEXITCODE

    if ($acceptanceExitCode -ne 0) {
        throw "Acceptance tests fallaron con exit code $acceptanceExitCode"
    }

    Write-Host '==> Smoke test completado exitosamente.'
}
finally {
    Write-Host '==> Deteniendo API en background...'
    if ($job) {
        try {
            Stop-Job -Job $job -ErrorAction SilentlyContinue
            Receive-Job -Job $job -ErrorAction SilentlyContinue | Out-Host
            Remove-Job -Job $job -Force -ErrorAction SilentlyContinue
        }
        catch {
        }
    }

    Set-Location $root

    if (-not $KeepEnvironment) {
        Write-Host '==> Apagando base de pruebas...'
        docker compose --env-file .env.test -f deployment/docker-compose.test.yml down | Out-Host
    }
}

exit $acceptanceExitCode
