$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$mainOut = Join-Path $projectRoot "out\main"
$resources = Join-Path $projectRoot "src\main\resources"
$sourceRoot = Join-Path $projectRoot "src\main\java"

New-Item -ItemType Directory -Force -Path $mainOut | Out-Null

$sources = Get-ChildItem -Path $sourceRoot -Recurse -Filter *.java
javac -d $mainOut $sources.FullName

java -cp "$mainOut;$resources" com.mazemind.MazeMindApp
