$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'ArrakisDevWorld.ps1')
$testRoot = Join-Path ([IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../build'))) ('launcher-test-' + [guid]::NewGuid())
New-Item -ItemType Directory -Path $testRoot | Out-Null
Set-Content -LiteralPath (Join-Path $testRoot 'gradle.properties') -Value 'mod_version=0.6.0-dev.2.1'
$plan = Get-ArrakisDevWorldPlan $testRoot '070926'
if ($plan.Folder -ne 'Arrakis-dev_0_6_0-dev_2_1_070926') { throw 'Incorrect Minecraft folder normalization.' }
if (!(Initialize-ArrakisDevWorld $plan)) { throw 'New world should request creation.' }
New-Item -ItemType Directory -Path $plan.SavePath -Force | Out-Null
Set-Content -LiteralPath (Join-Path $plan.SavePath 'level.dat') -Value 'simulated saved world'
$other = Join-Path $plan.SaveRoot ($plan.Folder + ' (1)')
New-Item -ItemType Directory -Path $other | Out-Null
if (Initialize-ArrakisDevWorld $plan) { throw 'Existing world should reopen.' }
if (!(Initialize-ArrakisDevWorld $plan -Fresh)) { throw 'Fresh should request creation.' }
if (Test-Path -LiteralPath $plan.SavePath) { throw 'Fresh did not remove the exact world.' }
if (!(Test-Path -LiteralPath $other)) { throw 'Fresh touched a different world.' }
New-Item -ItemType Directory -Path $plan.SavePath | Out-Null
if (!(Initialize-ArrakisDevWorld $plan)) { throw 'Incomplete world was not cleaned.' }
Set-Content -LiteralPath (Join-Path $testRoot 'gradle.properties') -Value 'mod_version=../../outside'
try { Get-ArrakisDevWorldPlan $testRoot '070926'; throw 'Unsafe version accepted.' }
catch { if ($_.Exception.Message -eq 'Unsafe version accepted.') { throw } }
Write-Host 'Launcher tests passed: create, reopen, fresh, collision isolation, incomplete save and unsafe version.'
