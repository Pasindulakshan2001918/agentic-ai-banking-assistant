# =====================================================================
# run-mobile.ps1 -- Launch Customer Banking Mobile App
# =====================================================================
param(
    [ValidateSet("chrome", "windows", "edge")]
    [string]$Device = "chrome"
)

$mobileDir = Join-Path $PSScriptRoot "agentic-ai-banking-assistant-mobile"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Launching Agentic Bank Customer Mobile App ($Device)     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Target Device: $Device" -ForegroundColor Yellow
Write-Host "Directory:     $mobileDir" -ForegroundColor Gray
Write-Host ""
Write-Host "Tips:" -ForegroundColor White
Write-Host "  • In the running terminal, press 'r' for Hot Reload" -ForegroundColor Gray
Write-Host "  • Press 'R' for Hot Restart" -ForegroundColor Gray
Write-Host "  • Press 'q' to quit" -ForegroundColor Gray
Write-Host ""

Set-Location $mobileDir
& flutter run -d $Device
