$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$stateFile = Join-Path $root '.local\dev-processes.json'

if (-not (Test-Path -LiteralPath $stateFile)) {
    'No development stack state file found.'
    exit 1
}

$state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
$rows = foreach ($service in $state.Services) {
    try {
        $response = Invoke-RestMethod -Uri $service.HealthUrl -TimeoutSec 3
        [pscustomobject]@{
            Service = $service.Name
            Port = $service.Port
            Status = $response.status
            Pid = $service.Pid
            Url = $service.Url
        }
    } catch {
        [pscustomobject]@{
            Service = $service.Name
            Port = $service.Port
            Status = 'DOWN'
            Pid = $service.Pid
            Url = $service.Url
        }
    }
}
$rows | Format-Table -AutoSize
if ($rows.Status -contains 'DOWN') { exit 1 }
