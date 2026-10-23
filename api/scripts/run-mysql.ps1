param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$AppArgs
)

$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $PSScriptRoot
$secretPath = Join-Path $projectDir '.local\mysql.password'
$repoPath = Join-Path $projectDir '.local\maven-repository'

if (-not (Test-Path -LiteralPath $secretPath)) {
    throw 'Missing .local\mysql.password. Provision the local MySQL credential before starting.'
}

$env:AGENT_DB_USERNAME = 'agent_gateway'
$env:AGENT_DB_PASSWORD = (Get-Content -LiteralPath $secretPath -Raw).Trim()

Push-Location $projectDir
try {
    $mavenArgs = @(
        "-Dmaven.repo.local=$repoPath",
        'spring-boot:run'
    ) + $AppArgs
    & mvn.cmd @mavenArgs
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
