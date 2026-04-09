param(
    [string]$ServerHost = "127.0.0.1",
    [int]$ServerPort = 61000,
    [string]$Profile = "admin",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $root "out\wifi-chat-admin.jar"

if (-not $SkipBuild) {
    Write-Host "Running build before launch..."
    & (Join-Path $PSScriptRoot "build.ps1") -SkipTests
}

if (!(Test-Path $jarPath)) {
    throw "Admin jar not found: $jarPath"
}

$args = @()
if ($ServerHost -ne "") { $args += "--server-host"; $args += $ServerHost }
if ($ServerPort -gt 0) { $args += "--server-port"; $args += $ServerPort }
if ($Profile -ne "") { $args += "--profile"; $args += $Profile }

java -jar $jarPath @args
