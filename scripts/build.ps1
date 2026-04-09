param(
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root "out"
$clientJarOut = Join-Path $outDir "wifi-chat-client.jar"
$serverJarOut = Join-Path $outDir "wifi-chat-server.jar"
$adminJarOut = Join-Path $outDir "wifi-chat-admin.jar"

function Resolve-MavenCommand {
    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    if ($null -ne $mvn) {
        return $mvn.Source
    }

    $candidates = @()
    if ($env:MAVEN_HOME) {
        $candidates += (Join-Path $env:MAVEN_HOME "bin\mvn.cmd")
    }
    if ($env:M2_HOME) {
        $candidates += (Join-Path $env:M2_HOME "bin\mvn.cmd")
    }

    $candidates += @(
        "D:\Tools\apache-maven-3.9.9\bin\mvn.cmd",
        "D:\Tools\apache-maven-3.9.14\bin\mvn.cmd",
        "C:\Tools\apache-maven-3.9.9\bin\mvn.cmd",
        "C:\Tools\apache-maven-3.9.14\bin\mvn.cmd"
    )

    foreach ($candidate in $candidates) {
        if ([string]::IsNullOrWhiteSpace($candidate)) {
            continue
        }
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    return $null
}

function Get-JarTool {
    $javacCmd = (Get-Command javac -ErrorAction Stop).Source
    $jarCmd = Join-Path (Split-Path $javacCmd -Parent) "jar.exe"
    if (Test-Path $jarCmd) {
        return @{ javac = $javacCmd; jar = $jarCmd }
    }

    $jarCandidate = Get-ChildItem -Path "C:\Program Files\Java" -Filter jar.exe -Recurse -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($null -eq $jarCandidate) {
        throw "jar.exe not found. Ensure JDK is installed."
    }
    return @{ javac = $javacCmd; jar = $jarCandidate.FullName }
}

function Build-WithMaven {
    param([string]$MvnCmd)

    Push-Location $root
    try {
        $mavenRepo = Join-Path $root ".m2repo"
        New-Item -ItemType Directory -Force -Path $mavenRepo | Out-Null

        $args = @("-Dmaven.repo.local=$mavenRepo", "-DskipTests=$($SkipTests.IsPresent)", "package")
        & $MvnCmd @args
        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed"
        }
    }
    finally {
        Pop-Location
    }

    $clientJar = Join-Path $root "client\target\wifi-chat-client.jar"
    $serverJar = Join-Path $root "server\target\wifi-chat-server.jar"
    $adminJar = Join-Path $root "admin\target\wifi-chat-admin.jar"
    if (!(Test-Path $clientJar) -or !(Test-Path $serverJar) -or !(Test-Path $adminJar)) {
        throw "Expected Maven jars not found in module target folders"
    }

    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    Copy-Item -Force $clientJar $clientJarOut
    Copy-Item -Force $serverJar $serverJarOut
    Copy-Item -Force $adminJar $adminJarOut
}

function Build-WithJavac {
    $tools = Get-JarTool
    $javac = $tools.javac
    $jar = $tools.jar

    $tmpRoot = Join-Path $outDir "tmp-build"
    $sharedClasses = Join-Path $tmpRoot "shared-classes"
    $serverClasses = Join-Path $tmpRoot "server-classes"
    $clientClasses = Join-Path $tmpRoot "client-classes"
    $adminClasses = Join-Path $tmpRoot "admin-classes"
    $serverStage = Join-Path $tmpRoot "server-stage"
    $clientStage = Join-Path $tmpRoot "client-stage"
    $adminStage = Join-Path $tmpRoot "admin-stage"

    Remove-Item -Recurse -Force $tmpRoot -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Force -Path $sharedClasses,$serverClasses,$clientClasses,$adminClasses,$serverStage,$clientStage,$adminStage,$outDir | Out-Null

    $sharedFiles = Get-ChildItem -Recurse -File (Join-Path $root "shared\src\main\java") -Filter *.java | ForEach-Object { $_.FullName }
    $serverFiles = Get-ChildItem -Recurse -File (Join-Path $root "server\src\main\java") -Filter *.java | ForEach-Object { $_.FullName }
    $clientFiles = Get-ChildItem -Recurse -File (Join-Path $root "client\src\main\java") -Filter *.java | ForEach-Object { $_.FullName }
    $adminFiles = Get-ChildItem -Recurse -File (Join-Path $root "admin\src\main\java") -Filter *.java | ForEach-Object { $_.FullName }

    if ($sharedFiles.Count -eq 0 -or $serverFiles.Count -eq 0 -or $clientFiles.Count -eq 0 -or $adminFiles.Count -eq 0) {
        throw "Missing Java source files in one or more modules"
    }

    & $javac --release 17 -encoding UTF-8 -d $sharedClasses $sharedFiles
    if ($LASTEXITCODE -ne 0) { throw "Shared compilation failed" }

    & $javac --release 17 -encoding UTF-8 -cp $sharedClasses -d $serverClasses $serverFiles
    if ($LASTEXITCODE -ne 0) { throw "Server compilation failed" }

    & $javac --release 17 -encoding UTF-8 -cp $sharedClasses -d $clientClasses $clientFiles
    if ($LASTEXITCODE -ne 0) { throw "Client compilation failed" }

    & $javac --release 17 -encoding UTF-8 -cp $sharedClasses -d $adminClasses $adminFiles
    if ($LASTEXITCODE -ne 0) { throw "Admin compilation failed" }

    Copy-Item -Recurse -Force (Join-Path $sharedClasses "*") $serverStage
    Copy-Item -Recurse -Force (Join-Path $serverClasses "*") $serverStage
    Copy-Item -Recurse -Force (Join-Path $sharedClasses "*") $clientStage
    Copy-Item -Recurse -Force (Join-Path $clientClasses "*") $clientStage
    Copy-Item -Recurse -Force (Join-Path $sharedClasses "*") $adminStage
    Copy-Item -Recurse -Force (Join-Path $adminClasses "*") $adminStage

    Remove-Item -Force $clientJarOut -ErrorAction SilentlyContinue
    Remove-Item -Force $serverJarOut -ErrorAction SilentlyContinue
    Remove-Item -Force $adminJarOut -ErrorAction SilentlyContinue

    & $jar --create --file $clientJarOut --main-class com.wifichat.Main -C $clientStage .
    if ($LASTEXITCODE -ne 0) { throw "Client jar packaging failed" }

    & $jar --create --file $serverJarOut --main-class com.wifichat.server.ServerMain -C $serverStage .
    if ($LASTEXITCODE -ne 0) { throw "Server jar packaging failed" }

    & $jar --create --file $adminJarOut --main-class com.wifichat.admin.AdminMain -C $adminStage .
    if ($LASTEXITCODE -ne 0) { throw "Admin jar packaging failed" }

    Write-Host "Built with javac fallback (Maven not found)."
    Write-Host "Note: server runtime still needs sqlite-jdbc on classpath if not shaded by Maven."
}

$mvnCmd = Resolve-MavenCommand
if ($null -ne $mvnCmd) {
    Build-WithMaven -MvnCmd $mvnCmd
}
else {
    Build-WithJavac
}

Write-Host "Build completed:"
Write-Host " -" $clientJarOut
Write-Host " -" $serverJarOut
Write-Host " -" $adminJarOut
