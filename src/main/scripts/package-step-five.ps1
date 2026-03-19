$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\\..\\..")).Path
$targetDir = Join-Path $projectRoot "target"
$zipDir = Join-Path $projectRoot "build"
$submissionDir = Join-Path $zipDir "submission-classes"
$zipPath = Join-Path $zipDir "step-five-classes.zip"
$submissionSources = @(
    "src\\main\\java\\Filter.java",
    "src\\main\\java\\TrueFilter.java",
    "src\\main\\java\\FourthRatings.java",
    "src\\main\\submission\\RecommendationRunner.java"
)
$submissionClasses = @(
    "Filter.class",
    "TrueFilter.class",
    "FourthRatings.class",
    "RecommendationRunner.class"
)

Push-Location $projectRoot
try {
    mvn -q -DskipTests package

    New-Item -ItemType Directory -Force -Path $zipDir | Out-Null
    if (Test-Path $submissionDir) {
        Remove-Item $submissionDir -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $submissionDir | Out-Null

    $compileArgs = @(
        "--release", "8",
        "-cp", (Join-Path $targetDir "classes"),
        "-d", $submissionDir
    ) + $submissionSources

    & javac @compileArgs
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed while compiling submission classes."
    }

    if (Test-Path $zipPath) {
        Remove-Item $zipPath -Force
    }

    $classPaths = foreach ($classFile in $submissionClasses) {
        $fullPath = Join-Path $submissionDir $classFile
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
