$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\\..\\..")).Path
$targetDir = Join-Path $projectRoot "target"
$zipDir = Join-Path $projectRoot "build"
$zipPath = Join-Path $zipDir "step-five-classes.zip"

Push-Location $projectRoot
try {
    mvn -q -DskipTests package

    New-Item -ItemType Directory -Force -Path $zipDir | Out-Null
    if (Test-Path $zipPath) {
        Remove-Item $zipPath -Force
    }

    Compress-Archive -Path (Join-Path $targetDir "classes\\*.class") -DestinationPath $zipPath
    Write-Host "Created $zipPath"
} finally {
    Pop-Location
}
