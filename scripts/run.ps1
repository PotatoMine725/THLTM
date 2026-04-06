param(
    [string]$Name = "",
    [string]$Iface = "",
    [string]$Group = "239.255.50.10",
    [int]$Port = 50000,
    [int]$PrivatePort = 0,
    [string]$Room = "General",
    [string]$Mode = "hybrid",
    [string]$ServerHost = "127.0.0.1",
    [int]$ServerPort = 61000,
    [string]$Profile = "default",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $root "out\wifi-chat-client.jar"

if (-not $SkipBuild) {
    Write-Host "Running build before launch..."
    & (Join-Path $PSScriptRoot "build.ps1") -SkipTests
}

if (!(Test-Path $jarPath)) {
    throw "Client jar not found: $jarPath"
}

$args = @()
if ($Name -ne "") { $args += "--name"; $args += $Name }
if ($Iface -ne "") { $args += "--iface"; $args += $Iface }
if ($Group -ne "") { $args += "--group"; $args += $Group }
if ($Port -gt 0) { $args += "--port"; $args += $Port }
if ($PrivatePort -gt 0) { $args += "--private-port"; $args += $PrivatePort }
if ($Room -ne "") { $args += "--room"; $args += $Room }
if ($Mode -ne "") { $args += "--mode"; $args += $Mode }
if ($ServerHost -ne "") { $args += "--server-host"; $args += $ServerHost }
if ($ServerPort -gt 0) { $args += "--server-port"; $args += $ServerPort }
if ($Profile -ne "") { $args += "--profile"; $args += $Profile }

java -jar $jarPath @args

