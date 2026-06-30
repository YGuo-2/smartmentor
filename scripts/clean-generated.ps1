param(
    [switch]$Apply
)

$ErrorActionPreference = "Stop"
$workspace = Split-Path -Parent $PSScriptRoot

$targets = @(
    "SmartMentor\target",
    "SmartMentor\build",
    "SmartMentor\.gradle",
    "smartmentor-web\dist",
    "smartmentor-web\node_modules",
    "smartmentor-web\.vite"
)

foreach ($relative in $targets) {
    $path = Join-Path $workspace $relative
    $resolvedParent = Resolve-Path -LiteralPath (Split-Path -Parent $path) -ErrorAction SilentlyContinue
    if ($null -eq $resolvedParent) {
        continue
    }
    $fullPath = Join-Path $resolvedParent.Path (Split-Path -Leaf $path)
    if (-not $fullPath.StartsWith($workspace, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to clean outside workspace: $fullPath"
    }
    if (Test-Path -LiteralPath $fullPath) {
        if ($Apply) {
            Remove-Item -LiteralPath $fullPath -Recurse -Force
            Write-Host "removed $relative"
        } else {
            Write-Host "would remove $relative"
        }
    }
}

if (-not $Apply) {
    Write-Host "Dry run only. Re-run with -Apply to remove generated directories."
}
