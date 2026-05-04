param(
    [Parameter(Mandatory = $true)]
    [string]$RepoRoot
)

$ErrorActionPreference = "Stop"

function Increment-Version {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Version
    )

    $parts = $Version.Split('.') | ForEach-Object { [int]$_ }
    if ($parts.Count -eq 0) {
        throw "Version '$Version' is invalid."
    }

    $index = $parts.Count - 1
    $parts[$index]++

    while ($index -gt 0 -and $parts[$index] -gt 9) {
        $parts[$index] = 0
        $index--
        $parts[$index]++
    }

    if ($parts[0] -gt 9) {
        $parts = ,$parts[0] + $parts[1..($parts.Count - 1)]
    }

    return ($parts -join '.')
}

function Replace-Or-AppendLine {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Pattern,
        [Parameter(Mandatory = $true)]
        [string]$Replacement
    )

    $content = Get-Content -LiteralPath $Path -Raw
    if ($content -match $Pattern) {
        $updated = [regex]::Replace($content, $Pattern, $Replacement, 1)
    } else {
        $separator = if ($content.EndsWith("`n") -or $content.Length -eq 0) { "" } else { "`r`n" }
        $updated = $content + $separator + $Replacement + "`r`n"
    }
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $updated, $utf8NoBom)
}

$versionFile = Join-Path $RepoRoot "version.properties"
$clientPropsFile = Join-Path $RepoRoot "client-mod\gradle.properties"
$readmeFile = Join-Path $RepoRoot "README.md"

if (-not (Test-Path -LiteralPath $versionFile)) {
    throw "Missing version file: $versionFile"
}

$versionContent = Get-Content -LiteralPath $versionFile -Raw
if ($versionContent -notmatch '(?m)^project_version=(.+)$') {
    throw "version.properties is missing project_version."
}

$currentVersion = $Matches[1].Trim()
$nextVersion = Increment-Version -Version $currentVersion

Replace-Or-AppendLine -Path $versionFile -Pattern '(?m)^project_version=.*$' -Replacement "project_version=$nextVersion"

if (Test-Path -LiteralPath $clientPropsFile) {
    Replace-Or-AppendLine -Path $clientPropsFile -Pattern '(?m)^mod_version=.*$' -Replacement "mod_version=$nextVersion"
}

if (Test-Path -LiteralPath $readmeFile) {
    $readme = Get-Content -LiteralPath $readmeFile -Raw
    $readme = [regex]::Replace($readme, 'version-\d+(?:\.\d+)+', "version-$nextVersion")
    Set-Content -LiteralPath $readmeFile -Value $readme -Encoding UTF8
}

Write-Host "Version bumped: $currentVersion -> $nextVersion"
