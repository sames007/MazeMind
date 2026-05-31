$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$mainOut = Join-Path $projectRoot "out\main"
$testOut = Join-Path $projectRoot "out\test"
$resources = Join-Path $projectRoot "src\main\resources"
$mainSourceRoot = Join-Path $projectRoot "src\main\java"
$testSourceRoot = Join-Path $projectRoot "src\test\java"

New-Item -ItemType Directory -Force -Path $mainOut, $testOut | Out-Null

$mainSources = Get-ChildItem -Path $mainSourceRoot -Recurse -Filter *.java
$testSources = Get-ChildItem -Path $testSourceRoot -Recurse -Filter *.java

javac -d $mainOut $mainSources.FullName
javac -cp "$mainOut;$resources" -d $testOut $testSources.FullName
java -cp "$mainOut;$testOut;$resources" com.mazemind.DocumentationMediaGenerator
