param([switch]$Fresh, [switch]$PlanOnly)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'ArrakisDevWorld.ps1')
try {
    $plan = Get-ArrakisDevWorldPlan -ProjectRoot (Split-Path $PSScriptRoot -Parent) -DateStamp (Get-Date -Format ddMMyy)
    if ($PlanOnly) { $plan | ConvertTo-Json; exit 0 }
    $create = Initialize-ArrakisDevWorld -Plan $plan -Fresh:$Fresh
    Write-Host "Arrakis $($plan.Version), save $($plan.Folder), seed 0"
    Push-Location $plan.ProjectRoot
    try {
        $launchArgs = @('runClient', "-PdevWorldName=$($plan.Folder)")
        if ($create) { $launchArgs += @('-PdevWorldCreate=true', '-PdevWorldSeed=0') }
        & '.\gradlew.bat' @launchArgs
        exit $LASTEXITCODE
    } finally { Pop-Location }
} catch { Write-Error $_; exit 1 }
