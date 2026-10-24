# ============================================================
# start.ps1  —  Agentic Banking Assistant: one-command startup
# Run: PowerShell -ExecutionPolicy Bypass -File .\start.ps1
# ============================================================
param(
    [switch]$SkipDocker,
    [switch]$NoUI
)

$ErrorActionPreference = "Stop"
$root    = $PSScriptRoot
$coreApi = Join-Path $root "core-api"
$agent   = Join-Path $root "mcp-agent"
$ui      = Join-Path $root "chat-ui\index.html"

Write-Host ""
Write-Host "  Agentic Digital Banking Assistant" -ForegroundColor Cyan
Write-Host ""

# ── 1. Postgres via Docker ────────────────────────────────────────────────
if (-not $SkipDocker) {
    Write-Host "[1/4] Starting Postgres..." -ForegroundColor Yellow
    $existing = docker ps -a --filter "name=bankdb" --format "{{.Names}}" 2>$null
    if ($existing -eq "bankdb") {
        $running = docker ps --filter "name=bankdb" --format "{{.Names}}" 2>$null
        if ($running -ne "bankdb") {
            docker start bankdb | Out-Null
            Write-Host "      Restarted existing bankdb container." -ForegroundColor Gray
        } else {
            Write-Host "      bankdb already running." -ForegroundColor Gray
        }
    } else {
        docker run --name bankdb `
            -e POSTGRES_USER=bank -e POSTGRES_PASSWORD=bank -e POSTGRES_DB=bank `
            -p 5433:5432 -d postgres:16 | Out-Null
        Write-Host "      Created new bankdb container (port 5433)." -ForegroundColor Gray
        Write-Host "      Waiting for Postgres to initialise..." -ForegroundColor Gray
        Start-Sleep -Seconds 5
    }
    Write-Host "  [OK] Postgres ready." -ForegroundColor Green
} else {
    Write-Host "[1/4] Skipping Docker (--SkipDocker flag set)." -ForegroundColor Gray
}

# ── 2. Spring Boot core API ───────────────────────────────────────────────
Write-Host ""
Write-Host "[2/4] Starting Spring Boot core API on port 8081..." -ForegroundColor Yellow
$javaProc = Start-Process -FilePath "mvn" `
    -ArgumentList "spring-boot:run" `
    -WorkingDirectory $coreApi `
    -PassThru -WindowStyle Minimized
Write-Host "      PID: $($javaProc.Id)" -ForegroundColor Gray

Write-Host "      Waiting for API to be ready (up to 60s)..." -ForegroundColor Gray
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 2
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8081/api/v1/admin/login" `
             -Method OPTIONS -ErrorAction SilentlyContinue -UseBasicParsing
        if ($r.StatusCode -in 200, 400, 401, 405) { $ready = $true; break }
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -in 200, 400, 401, 405) { $ready = $true; break }
    }
}
if (-not $ready) {
    Write-Host "  [WARN] Spring Boot did not respond in 60s. Check the minimised window." -ForegroundColor Red
} else {
    Write-Host "  [OK] Spring Boot is running." -ForegroundColor Green
}

# ── 3. Python MCP Agent gateway ───────────────────────────────────────────
Write-Host ""
Write-Host "[3/4] Starting Python agent gateway on port 8290..." -ForegroundColor Yellow

$pyExe = Join-Path $agent ".venv\Scripts\python.exe"
if (-not (Test-Path $pyExe)) { $pyExe = "python" }

$agentProc = Start-Process -FilePath $pyExe `
    -ArgumentList "gateway.py" `
    -WorkingDirectory $agent `
    -PassThru -WindowStyle Minimized

Write-Host "      PID: $($agentProc.Id)" -ForegroundColor Gray
Start-Sleep -Seconds 4

$gatewayReady = $false
for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Seconds 1
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8290/health" -UseBasicParsing -ErrorAction SilentlyContinue
        if ($r.StatusCode -eq 200) { $gatewayReady = $true; break }
    } catch { }
}
if (-not $gatewayReady) {
    Write-Host "  [WARN] Gateway did not respond. Check the minimised window." -ForegroundColor Red
} else {
    Write-Host "  [OK] Agent gateway is running." -ForegroundColor Green
}

# ── 4. Open chat UI ───────────────────────────────────────────────────────
Write-Host ""
if (-not $NoUI -and (Test-Path $ui)) {
    Write-Host "[4/4] Opening chat UI in your browser..." -ForegroundColor Yellow
    Start-Process $ui
    Write-Host "  [OK] Browser opened." -ForegroundColor Green
} else {
    Write-Host "[4/4] Chat UI: open $ui manually." -ForegroundColor Gray
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  All services started!" -ForegroundColor Green
Write-Host "  Spring Boot : http://localhost:8081" -ForegroundColor White
Write-Host "  Agent API   : http://localhost:8290" -ForegroundColor White
Write-Host "  Chat UI     : $ui" -ForegroundColor White
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Press Ctrl+C to stop everything." -ForegroundColor Gray
Write-Host "  Or close the two minimised PowerShell windows manually." -ForegroundColor Gray
Write-Host ""

try { while ($true) { Start-Sleep -Seconds 60 } }
finally {
    Write-Host "Stopping services..." -ForegroundColor Yellow
    try { Stop-Process -Id $javaProc.Id  -ErrorAction SilentlyContinue } catch {}
    try { Stop-Process -Id $agentProc.Id -ErrorAction SilentlyContinue } catch {}
}