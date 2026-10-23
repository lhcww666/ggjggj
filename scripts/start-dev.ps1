$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$stateDir = Join-Path $root '.local'
$logDir = Join-Path $stateDir 'logs'
$stateFile = Join-Path $stateDir 'dev-processes.json'
$mavenRepo = Join-Path $root 'api\agent-gateway\.local\maven-repository'
New-Item -ItemType Directory -Path $stateDir, $logDir -Force | Out-Null

function Get-PortOwner([int]$port) {
    $line = & netstat.exe -ano | Select-String -Pattern ":$port\s+.*LISTENING\s+(\d+)$" | Select-Object -First 1
    if ($line -and $line.Line -match 'LISTENING\s+(\d+)$') {
        return [int]$Matches[1]
    }
    return $null
}

function Wait-Http([string]$url, [int]$seconds) {
    for ($i = 0; $i -lt $seconds; $i++) {
        Start-Sleep -Seconds 1
        try {
            $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) { return $true }
        } catch {}
    }
    return $false
}

function Start-Background([string]$file, [string[]]$arguments, [string]$workingDirectory,
                         [string]$stdout, [string]$stderr) {
    return Start-Process -FilePath $file -ArgumentList $arguments `
        -WorkingDirectory $workingDirectory -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr
}

foreach ($port in @(18080, 18081, 9000)) {
    if (Get-PortOwner $port) {
        throw "Port $port is already in use. Run scripts/status-dev.ps1 or stop the other process."
    }
}

$node = (Get-Command node.exe -ErrorAction Stop).Source
$mvn = (Get-Command mvn.cmd -ErrorAction Stop).Source
$launchers = @()

try {
    $launchers += Start-Background 'powershell.exe' @(
        '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
        (Join-Path $root 'api\agent-gateway\scripts\run-mysql.ps1')
    ) (Join-Path $root 'api\agent-gateway') `
      (Join-Path $logDir 'agent.out.log') (Join-Path $logDir 'agent.err.log')

    $launchers += Start-Background $mvn @(
        "-Dmaven.repo.local=$mavenRepo", 'spring-boot:run'
    ) (Join-Path $root 'api\business-integrations') `
      (Join-Path $logDir 'business.out.log') (Join-Path $logDir 'business.err.log')

    $launchers += Start-Background $node @('server.mjs') `
        (Join-Path $root 'ui') (Join-Path $logDir 'ui.out.log') (Join-Path $logDir 'ui.err.log')

    $agentReady = Wait-Http 'http://127.0.0.1:18080/api/health' 120
    $businessReady = Wait-Http 'http://127.0.0.1:18081/api/health' 120
    $uiReady = Wait-Http 'http://127.0.0.1:9000/health' 60
    if (-not ($agentReady -and $businessReady -and $uiReady)) {
        throw "Services did not become healthy. Logs: $logDir"
    }

    $services = @(
        [pscustomobject]@{ Name='ui'; Port=9000; Url='http://localhost:9000/'; HealthUrl='http://localhost:9000/health'; Pid=(Get-PortOwner 9000) },
        [pscustomobject]@{ Name='agent-api'; Port=18080; Url='http://localhost:18080/api/health'; HealthUrl='http://localhost:18080/api/health'; Pid=(Get-PortOwner 18080) },
        [pscustomobject]@{ Name='business-api'; Port=18081; Url='http://localhost:18081/api/health'; HealthUrl='http://localhost:18081/api/health'; Pid=(Get-PortOwner 18081) }
    )
    [pscustomobject]@{
        StartedAt = (Get-Date).ToString('o')
        LauncherPids = @($launchers | ForEach-Object { $_.Id })
        Services = $services
        Logs = $logDir
    } | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $stateFile -Encoding UTF8

    'Development stack is ready.'
    $services | Format-Table Name, Port, Url -AutoSize
} catch {
    foreach ($port in @(18080, 18081, 9000)) {
        $owner = Get-PortOwner $port
        if ($owner) {
            try { Stop-Process -Id $owner -Force -ErrorAction Stop } catch {}
        }
    }
    foreach ($launcher in $launchers) {
        try { Stop-Process -Id $launcher.Id -Force -ErrorAction Stop } catch {}
    }
    throw
}
