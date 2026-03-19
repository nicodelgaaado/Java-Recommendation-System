$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\\..\\..")).Path
$targetDir = Join-Path $projectRoot "target"
$zipDir = Join-Path $projectRoot "build"
$zipPath = Join-Path $zipDir "step-five-classes.zip"
$legacySubmissionDir = Join-Path $zipDir "submission-classes"
$submissionClasses = @(
    "FourthRatings.class",
    "RecommendationRunner.class",
    "Filter.class",
    "AllFilters.class",
    "TrueFilter.class",
    "YearAfterFilter.class",
    "MinutesFilter.class"
)

Push-Location $projectRoot
try {
    mvn -q -DskipTests package

    New-Item -ItemType Directory -Force -Path $zipDir | Out-Null
    if (Test-Path $legacySubmissionDir) {
        Remove-Item $legacySubmissionDir -Recurse -Force
    }
    if (Test-Path $zipPath) {
        Remove-Item $zipPath -Force
    }

    $classPaths = foreach ($classFile in $submissionClasses) {
        $fullPath = Join-Path $targetDir "classes\\$classFile"
        if (-not (Test-Path $fullPath)) {
            throw "Missing compiled class for submission: $classFile"
        }
        $fullPath
    }

    Compress-Archive -Path $classPaths -DestinationPath $zipPath
    Write-Host "Created $zipPath"
} finally {
    Pop-Location
}
