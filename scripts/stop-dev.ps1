$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$stateFile = Join-Path $root '.local\dev-processes.json'

if (-not (Test-Path -LiteralPath $stateFile)) {
    'No development stack state file found.'
    exit 0
}

$state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
foreach ($service in $state.Services) {
    if ($service.Pid) {
        try {
            Stop-Process -Id ([int]$service.Pid) -Force -ErrorAction Stop
            "Stopped $($service.Name) pid=$($service.Pid)"
        } catch {}
    }
}
foreach ($pidValue in @($state.LauncherPids)) {
    if ($pidValue) {
        try { Stop-Process -Id ([int]$pidValue) -Force -ErrorAction Stop } catch {}
    }
}
Remove-Item -LiteralPath $stateFile -Force
'Development stack stopped.'
