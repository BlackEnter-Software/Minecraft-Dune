Set-StrictMode -Version Latest

function Get-ArrakisDevWorldPlan {
    param([string]$ProjectRoot, [string]$DateStamp)
    $project = [IO.Path]::GetFullPath($ProjectRoot)
    $versionLine = @(Get-Content -LiteralPath (Join-Path $project 'gradle.properties') |
        Where-Object { $_ -match '^mod_version=' })
    if ($versionLine.Count -ne 1) { throw 'Expected exactly one mod_version in gradle.properties.' }
    $version = $versionLine[0].Substring('mod_version='.Length).Trim()
    if ($version -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]*$' -or $DateStamp -notmatch '^\d{6}$') {
        throw 'Unsafe development version or date.'
    }
    # Minecraft FileUtil replaces dots with underscores in world folder names.
    $folder = "Arrakis-dev_$($version.Replace('.', '_'))_$DateStamp"
    $saveRoot = [IO.Path]::GetFullPath((Join-Path $project 'run/saves'))
    $savePath = [IO.Path]::GetFullPath((Join-Path $saveRoot $folder))
    if ([IO.Path]::GetDirectoryName($savePath) -ne $saveRoot) { throw 'Dev save escaped the saves directory.' }
    [pscustomobject]@{ ProjectRoot = $project; Version = $version; Folder = $folder
        SaveRoot = $saveRoot; SavePath = $savePath }
}

function Initialize-ArrakisDevWorld {
    param($Plan, [switch]$Fresh)
    foreach ($path in @((Join-Path $Plan.ProjectRoot 'run'), $Plan.SaveRoot, $Plan.SavePath)) {
        if ((Test-Path -LiteralPath $path) -and
            ((Get-Item -LiteralPath $path -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw "Refusing linked dev save path: $path"
        }
    }
    if ((Test-Path -LiteralPath $Plan.SavePath) -and
        ($Fresh -or !(Test-Path -LiteralPath (Join-Path $Plan.SavePath 'level.dat')))) {
        if ([IO.Path]::GetFullPath($Plan.SavePath) -ne (Join-Path $Plan.SaveRoot $Plan.Folder) -or
            $Plan.Folder -notmatch '^Arrakis-dev_[A-Za-z0-9_-]+_\d{6}$') {
            throw 'Refusing removal outside the exact dev save directory.'
        }
        if (@(Get-ChildItem -LiteralPath $Plan.SavePath -Recurse -Force |
                Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint }).Count -gt 0) {
            throw 'Refusing removal of a dev save containing links.'
        }
        Write-Host "Removing dev world: $($Plan.Folder)"
        Remove-Item -LiteralPath $Plan.SavePath -Recurse -Force -ErrorAction Stop
    }
    return !(Test-Path -LiteralPath (Join-Path $Plan.SavePath 'level.dat'))
}
