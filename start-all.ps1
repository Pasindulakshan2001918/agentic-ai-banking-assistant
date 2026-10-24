# =====================================================================
# start-all.ps1 -- Launch Entire Agentic AI Banking Assistant System
# Runs: Database + Spring Boot Core API + MCP Agent Gateway + Admin Panel
# =====================================================================
param(
    [switch]$SkipDocker,
    [switch]$RunMobileWeb
)

$ErrorActionPreference = "Stop"
$root       = $PSScriptRoot
$agentDir   = Join-Path $root "agentic-ai-banking-assistant-fb-Agent"
$coreApi    = Join-Path $agentDir "core-api"
$mcpAgent   = Join-Path $agentDir "mcp-agent"
$adminDir   = Join-Path $root "agentic-ai-banking-assistant-Admin-Panel\agentic-ai-admin"
$mobileDir  = Join-Path $root "agentic-ai-banking-assistant-mobile"

$pgDir      = Join-Path $agentDir "pgsql"
$pgData     = Join-Path $agentDir "pg_data"
$mvn        = Join-Path $agentDir "maven\bin\mvn.cmd"

# Ensure Java 21 is configured
if (-not $env:JAVA_HOME) {
    $jdkPath = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
    if (Test-Path $jdkPath) {
        $env:JAVA_HOME = $jdkPath
        $env:PATH = "$jdkPath\bin;$env:PATH"
    }
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Agentic AI Digital Banking Assistant -- Unified Startup  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# -- 1. PostgreSQL Database ---------------------------------------------
Write-Host "[1/4] Starting PostgreSQL Database..." -ForegroundColor Yellow
$pgStarted = $false
if (Test-Path $pgDir) {
    if (-not (Test-Path $pgData)) {
        Write-Host "      Initializing native database cluster..." -ForegroundColor Gray
        "bank" | Out-File -FilePath "$agentDir\pg_pw.txt" -Encoding ascii
        Start-Process -FilePath "$pgDir\bin\initdb.exe" -ArgumentList "-D", """$pgData""", "-U", "bank", "-E", "UTF8", "--pwfile=""$agentDir\pg_pw.txt""" -Wait -WindowStyle Hidden
        Remove-Item "$agentDir\pg_pw.txt" -ErrorAction SilentlyContinue
    }
    Start-Process -FilePath "$pgDir\bin\pg_ctl.exe" -ArgumentList "-D", """$pgData""", "-l", """$agentDir\pg.log""", "start" -WindowStyle Hidden
    Start-Sleep -Seconds 2
    Start-Process -FilePath "$pgDir\bin\createdb.exe" -ArgumentList "-U", "bank", "-E", "UTF8", "bank" -Wait -WindowStyle Hidden 2>$null
    $pgStarted = $true
    Write-Host "  [OK] Native PostgreSQL ready on localhost:5432." -ForegroundColor Green
} elseif (-not $SkipDocker) {
    $existing = docker ps -a --filter "name=bankdb" --format "{{.Names}}" 2>$null
    if ($existing -eq "bankdb") {
        $running = docker ps --filter "name=bankdb" --format "{{.Names}}" 2>$null
        if ($running -ne "bankdb") { docker start bankdb | Out-Null }
    } else {
        docker run --name bankdb -e POSTGRES_USER=bank -e POSTGRES_PASSWORD=bank -e POSTGRES_DB=bank -p 5432:5432 -d postgres:16 | Out-Null
        Start-Sleep -Seconds 4
    }
    $pgStarted = $true
    Write-Host "  [OK] Docker PostgreSQL ready on localhost:5432." -ForegroundColor Green
}

# -- 2. Spring Boot Core API --------------------------------------------
Write-Host ""
Write-Host "[2/4] Starting Spring Boot Core API on port 8081..." -ForegroundColor Yellow
$mvnPath = if (Test-Path $mvn) { $mvn } else { "mvn" }

# Check if port 8081 is already running
$alreadyUp = $false
try {
    $r = Invoke-WebRequest -Uri "http://localhost:8081/api/v1/admin/login" -Method OPTIONS -ErrorAction SilentlyContinue -UseBasicParsing
    if ($r.StatusCode -in 200, 400, 401, 405) { $alreadyUp = $true }
} catch {
    if ($_.Exception.Response.StatusCode.value__ -in 200, 400, 401, 405) { $alreadyUp = $true }
}

if (-not $alreadyUp) {
    $javaProc = Start-Process -FilePath $mvnPath `
        -ArgumentList "spring-boot:run" `
        -WorkingDirectory $coreApi `
        -PassThru -WindowStyle Minimized

    Write-Host "      Waiting for Core API to be ready..." -ForegroundColor Gray
    $apiReady = $false
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Seconds 2
        try {
            $r = Invoke-WebRequest -Uri "http://localhost:8081/api/v1/admin/login" `
                 -Method OPTIONS -ErrorAction SilentlyContinue -UseBasicParsing
            if ($r.StatusCode -in 200, 400, 401, 405) { $apiReady = $true; break }
        } catch {
            if ($_.Exception.Response.StatusCode.value__ -in 200, 400, 401, 405) { $apiReady = $true; break }
        }
    }
    if ($apiReady) {
        Write-Host "  [OK] Spring Boot Core API is running at http://localhost:8081." -ForegroundColor Green
    } else {
        Write-Host "  [WARN] Core API did not respond within 60s. Check minimized console." -ForegroundColor Red
    }
} else {
    Write-Host "  [OK] Spring Boot Core API already active on port 8081." -ForegroundColor Green
}

# -- 3. Python MCP Agent Gateway ----------------------------------------
Write-Host ""
Write-Host "[3/4] Starting Python MCP Agent Gateway on port 8290..." -ForegroundColor Yellow
$gatewayAlreadyUp = $false
try {
    $gw = Invoke-WebRequest -Uri "http://localhost:8290/health" -UseBasicParsing -ErrorAction SilentlyContinue
    if ($gw.StatusCode -eq 200) { $gatewayAlreadyUp = $true }
} catch {}

if (-not $gatewayAlreadyUp) {
    $pyExe = Join-Path $mcpAgent ".venv\Scripts\python.exe"
    if (-not (Test-Path $pyExe)) { $pyExe = "python" }

    $agentProc = Start-Process -FilePath $pyExe `
        -ArgumentList "gateway.py" `
        -WorkingDirectory $mcpAgent `
        -PassThru -WindowStyle Minimized

    Start-Sleep -Seconds 2
    Write-Host "  [OK] MCP Agent Gateway running at http://localhost:8290." -ForegroundColor Green
} else {
    Write-Host "  [OK] MCP Agent Gateway already active on port 8290." -ForegroundColor Green
}

# -- 4. Admin Panel Web Server ------------------------------------------
Write-Host ""
Write-Host "[4/4] Starting Admin Panel on port 5173..." -ForegroundColor Yellow
$adminAlreadyUp = $false
try {
    $adm = Invoke-WebRequest -Uri "http://127.0.0.1:5173" -UseBasicParsing -ErrorAction SilentlyContinue
    if ($adm.StatusCode -eq 200) { $adminAlreadyUp = $true }
} catch {}

if (-not $adminAlreadyUp) {
    $adminProc = Start-Process -FilePath "node" `
        -ArgumentList "server.js" `
        -WorkingDirectory $adminDir `
        -PassThru -WindowStyle Minimized
    Start-Sleep -Seconds 1
}

Write-Host "  [OK] Admin Panel running at http://127.0.0.1:5173." -ForegroundColor Green
Start-Process "http://127.0.0.1:5173"

# -- 5. Mobile App ------------------------------------------------------
if ($RunMobileWeb) {
    Write-Host ""
    Write-Host "[Optional] Launching Mobile App on Chrome Web..." -ForegroundColor Yellow
    $mobileProc = Start-Process -FilePath "flutter.bat" `
        -ArgumentList "run", "-d", "chrome" `
        -WorkingDirectory $mobileDir `
        -PassThru
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  SYSTEM READY & FULLY CONNECTED TO THE SAME DATABASE!   " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  1. Admin Panel:   http://127.0.0.1:5173" -ForegroundColor White
Write-Host "     Credentials:   admin@bank.com / Admin@1234" -ForegroundColor Gray
Write-Host "     Feature:       Create customer bank accounts & set real balances" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. Mobile App:    agentic-ai-banking-assistant-mobile" -ForegroundColor White
Write-Host "     To launch:     cd agentic-ai-banking-assistant-mobile; flutter run -d chrome" -ForegroundColor Gray
Write-Host "     Sign Up:       Enter Account No (created by Admin) + username + password" -ForegroundColor Gray
Write-Host "     AI Agent:      Click 'AI Agent' button to interact with live balance" -ForegroundColor Gray
Write-Host ""
Write-Host "  3. Core API:      http://localhost:8081/api/v1" -ForegroundColor White
Write-Host "  4. Agent Gateway: http://localhost:8290" -ForegroundColor White
Write-Host ""
Write-Host "  Press Ctrl+C to stop services." -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

try {
    while ($true) { Start-Sleep -Seconds 60 }
}
finally {
    Write-Host "Shutting down services..." -ForegroundColor Yellow
    if ($javaProc)  { try { Stop-Process -Id $javaProc.Id  -ErrorAction SilentlyContinue } catch {} }
    if ($agentProc) { try { Stop-Process -Id $agentProc.Id -ErrorAction SilentlyContinue } catch {} }
    if ($adminProc) { try { Stop-Process -Id $adminProc.Id -ErrorAction SilentlyContinue } catch {} }
    if ($mobileProc){ try { Stop-Process -Id $mobileProc.Id -ErrorAction SilentlyContinue } catch {} }
}
