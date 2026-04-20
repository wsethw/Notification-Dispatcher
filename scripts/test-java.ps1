param(
    [string]$Goal = "test",
    [string]$MavenVersion = "3.9.14"
)

$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..\notification-service-java")
$cacheRoot = Join-Path $env:TEMP "notification-dispatcher-tools"
$mavenRoot = Join-Path $cacheRoot "apache-maven-$MavenVersion"
$mavenExecutable = Join-Path $mavenRoot "bin\mvn.cmd"
$archivePath = Join-Path $cacheRoot "apache-maven-$MavenVersion-bin.zip"
$distributionUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MavenVersion/apache-maven-$MavenVersion-bin.zip"

New-Item -ItemType Directory -Force -Path $cacheRoot | Out-Null

if (-not (Test-Path $mavenExecutable)) {
    Write-Host "Downloading Apache Maven $MavenVersion..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $distributionUrl -OutFile $archivePath
    Expand-Archive -LiteralPath $archivePath -DestinationPath $cacheRoot -Force
}

Write-Host "Running mvn $Goal in $projectRoot" -ForegroundColor Cyan
Push-Location $projectRoot
try {
    & $mavenExecutable $Goal
}
finally {
    Pop-Location
}
