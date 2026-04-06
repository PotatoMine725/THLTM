param(
    [string]$ServerHost = "127.0.0.1",
    [int]$ServerPort = 61000,
    [string]$Group = "239.255.50.10",
    [int]$Port = 50000,
    [string]$Room = "General",
    [string]$AppVersion = "1.0.0",
    [switch]$SkipBuild,
    [switch]$NoInstaller
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root "out"
$clientJar = Join-Path $outDir "wifi-chat-client.jar"
$distDir = Join-Path $root "dist"
$portableDir = Join-Path $distDir "WiFiChatClient-portable"
$runtimeDir = Join-Path $portableDir "runtime"
$zipPath = Join-Path $distDir "WiFiChatClient-portable.zip"

function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\\java.exe"))) {
        return $env:JAVA_HOME
    }

    $settings = cmd /c "java -XshowSettings:properties -version 2>&1"
    $line = $settings | Where-Object { $_ -match "^\s*java.home\s*=\s*" } | Select-Object -First 1
    if (-not $line) {
        throw "Cannot resolve java.home. Please install JDK 17+ and make 'java' available."
    }

    $javaHome = ($line -split "=", 2)[1].Trim()
    if (-not (Test-Path (Join-Path $javaHome "bin\\java.exe"))) {
        throw "Resolved java.home is invalid: $javaHome"
    }
    return $javaHome
}

if (-not $SkipBuild) {
    Write-Host "Building jars..."
    & (Join-Path $PSScriptRoot "build.ps1") -SkipTests
}

if (!(Test-Path $clientJar)) {
    throw "Client jar not found: $clientJar"
}

$javaHome = Resolve-JavaHome
$jlinkExe = Join-Path $javaHome "bin\\jlink.exe"
$jpackageExe = Join-Path $javaHome "bin\\jpackage.exe"
if (!(Test-Path $jlinkExe)) {
    throw "jlink not found at $jlinkExe. Please use full JDK (not JRE)."
}

Write-Host "Preparing dist folder..."
Remove-Item -Recurse -Force $portableDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $portableDir,$distDir | Out-Null
Copy-Item -Force $clientJar (Join-Path $portableDir "wifi-chat-client.jar")

Write-Host "Creating minimal runtime image..."
if (Test-Path $runtimeDir) {
    Remove-Item -Recurse -Force $runtimeDir
}
& $jlinkExe `
    --add-modules java.base,java.desktop,java.logging,java.naming `
    --output $runtimeDir `
    --strip-debug `
    --no-man-pages `
    --no-header-files `
    --compress=2
if ($LASTEXITCODE -ne 0) {
    throw "jlink failed."
}

$startBat = @"
@echo off
setlocal
set DIR=%~dp0
set PROFILE=%COMPUTERNAME%
if not "%~1"=="" set PROFILE=%~1
if not "%~1"=="" shift
"%DIR%runtime\\bin\\javaw.exe" -jar "%DIR%wifi-chat-client.jar" --mode hybrid --server-host $ServerHost --server-port $ServerPort --group $Group --port $Port --room "$Room" --profile "%PROFILE%" %*
"@
Set-Content -Path (Join-Path $portableDir "start-client.bat") -Value $startBat -Encoding ASCII

$startCliBat = @"
@echo off
setlocal
set DIR=%~dp0
set PROFILE=%COMPUTERNAME%
if not "%~1"=="" set PROFILE=%~1
if not "%~1"=="" shift
"%DIR%runtime\\bin\\java.exe" -jar "%DIR%wifi-chat-client.jar" --mode hybrid --server-host $ServerHost --server-port $ServerPort --group $Group --port $Port --room "$Room" --profile "%PROFILE%" %*
pause
"@
Set-Content -Path (Join-Path $portableDir "start-client-cli.bat") -Value $startCliBat -Encoding ASCII

$readme = @"
WiFi Chat Client Portable
========================

1) Double-click start-client.bat
2) If asked profile/account, use a unique one per person
3) This build is preconfigured to connect:
   - ServerHost: $ServerHost
   - ServerPort: $ServerPort
   - Group: $Group
   - Port: $Port
   - Room: $Room

Notes:
- No need to install Java on client machine (runtime included).
- To force unique profile manually:
  start-client.bat PROFILE_NAME
"@
Set-Content -Path (Join-Path $portableDir "README-portable.txt") -Value $readme -Encoding ASCII

if (Test-Path $zipPath) {
    Remove-Item -Force $zipPath
}
Compress-Archive -Path (Join-Path $portableDir "*") -DestinationPath $zipPath

if (-not $NoInstaller -and (Test-Path $jpackageExe)) {
    Write-Host "Creating Windows installer (.exe)..."
    $appArgs = "--mode hybrid --server-host $ServerHost --server-port $ServerPort --group $Group --port $Port --room $Room"

    & $jpackageExe `
        --type exe `
        --name "WiFiChatClient" `
        --input $portableDir `
        --main-jar "wifi-chat-client.jar" `
        --main-class "com.wifichat.Main" `
        --runtime-image $runtimeDir `
        --dest $distDir `
        --win-shortcut `
        --win-menu `
        --app-version $AppVersion `
        --arguments $appArgs

    if ($LASTEXITCODE -ne 0) {
        Write-Warning "jpackage failed. Portable zip is still available."
    }
}
else {
    Write-Host "Skipping installer creation (NoInstaller switch or jpackage missing)."
}

Write-Host "Done. Artifacts:"
Write-Host " - Portable folder: $portableDir"
Write-Host " - Portable zip:    $zipPath"
$installer = Join-Path $distDir "WiFiChatClient-$AppVersion.exe"
if (Test-Path $installer) {
    Write-Host " - Installer:       $installer"
}
