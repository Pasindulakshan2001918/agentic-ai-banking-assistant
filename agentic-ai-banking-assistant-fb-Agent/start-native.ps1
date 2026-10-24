# ============================================================
# start-native.ps1  —  Agentic Banking Assistant native startup
# Runs everything without Docker!
# ============================================================
$ErrorActionPreference = "Stop"
$root    = $PSScriptRoot
$coreApi = Join-Path $root "core-api"
$agent   = Join-Path $root "mcp-agent"
$ui      = Join-Path $root "chat-ui\index.html"
$pgDir   = Join-Path $root "pgsql"
$pgData  = Join-Path $root "pg_data"
$mvn     = Join-Path $root "maven\bin\mvn.cmd"

Write-Host ""
Write-Host "  Agentic Digital Banking Assistant (Native Setup)" -ForegroundColor Cyan
Write-Host ""

# ── 1. PostgreSQL Native ────────────────────────────────────────────────
if (Test-Path $pgDir) {
    Write-Host "[1/4] Starting PostgreSQL natively..." -ForegroundColor Yellow
    if (-not (Test-Path $pgData)) {
        Write-Host "      Initializing database..." -ForegroundColor Gray
        "bank" | Out-File -FilePath "$root\pg_pw.txt" -Encoding ascii
        Start-Process -FilePath "$pgDir\bin\initdb.exe" -ArgumentList "-D", """$pgData""", "-U", "bank", "-E", "UTF8", "--pwfile=""$root\pg_pw.txt""" -Wait -WindowStyle Hidden
        Remove-Item "$root\pg_pw.txt"
    }
    
    # Start pg
    Start-Process -FilePath "$pgDir\bin\pg_ctl.exe" -ArgumentList "-D", """$pgData""", "-l", """$root\pg.log""", "start" -WindowStyle Hidden
    Write-Host "      Waiting for Postgres to initialise..." -ForegroundColor Gray
    Start-Sleep -Seconds 3
    
    # Ensure 'bank' database exists
    Start-Process -FilePath "$pgDir\bin\createdb.exe" -ArgumentList "-U", "bank", "-E", "UTF8", "bank" -Wait -WindowStyle Hidden

    Write-Host "  [OK] Postgres ready." -ForegroundColor Green
} else {
    Write-Host "[1/4] PostgreSQL binary folder 'pgsql' not found!" -ForegroundColor Red
}

# ── 2. Spring Boot core API ───────────────────────────────────────────────
Write-Host ""
Write-Host "[2/4] Starting Spring Boot core API on port 8081..." -ForegroundColor Yellow
if (Test-Path $mvn) {
    $javaProc = Start-Process -FilePath $mvn `
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
} else {
    Write-Host "  [ERROR] Maven not found at $mvn" -ForegroundColor Red
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
if (Test-Path $ui) {
    Write-Host "[4/4] Opening chat UI in your browser..." -ForegroundColor Yellow
    Start-Process $ui
    Write-Host "  [OK] Browser opened." -ForegroundColor Green
} else {
    Write-Host "[4/4] Chat UI: open $ui manually." -ForegroundColor Gray
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  All services started natively!" -ForegroundColor Green
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
    if (Test-Path "$pgDir\bin\pg_ctl.exe") {
        Write-Host "Stopping PostgreSQL..." -ForegroundColor Yellow
        Start-Process -FilePath "$pgDir\bin\pg_ctl.exe" -ArgumentList "-D", """$pgData""", "stop" -Wait -WindowStyle Hidden
    }
}
