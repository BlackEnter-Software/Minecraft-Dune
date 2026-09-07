$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
# A unique folder avoids deleting or reusing any existing player world or report.
$folder = 'Arrakis-validation_' + (Get-Date -Format yyyyMMdd_HHmmss) + '_' + [guid]::NewGuid().ToString('N').Substring(0,8)
$output = Join-Path $project "build/terrain-validation-client/validation/$folder"
Push-Location $project
try {
    foreach ($phase in @('generate', 'reload')) {
        & '.\gradlew.bat' runTerrainValidationClient "-PterrainValidationPhase=$phase" "-PterrainValidationFolder=$folder"
        if ($LASTEXITCODE -ne 0 -or !(Test-Path -LiteralPath (Join-Path $output "$phase.passed"))) {
            throw "Runtime validation failed during $phase. Inspect $output and build/terrain-validation-client/logs/latest.log"
        }
    }
    Write-Host "Generation and save/reload checks passed. Screenshots and reports: $output"
} finally { Pop-Location }
