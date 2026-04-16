param(
    [Parameter(Mandatory = $true)]
    [Alias("Host")]
    [string]$ServerIp,

    [string]$User = "root",
    [int]$Port = 22,
    [string]$KeyPath = "",

    [int]$AppPort = 61000,
    [string]$DbPath = "/var/lib/wifichat/chat.db",
    [string]$RemoteDir = "/opt/wifichat",

    [switch]$SkipBuild,
    [switch]$SetupFirewall,
    [switch]$OpenUdpDiscovery,
    [int]$UdpPort = 50000
)

$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    return Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
}

function New-BashLiteral {
    param([string]$Value)
    return "'" + ($Value -replace "'", "'""'""'") + "'"
}

$repoRoot = Get-RepoRoot
$jarPath = Join-Path $repoRoot "out\wifi-chat-server.jar"
$buildScript = Join-Path $repoRoot "scripts\build.ps1"
$tmpDir = Join-Path $repoRoot "out\tmp-do-deploy"

if (-not $SkipBuild) {
    Write-Host "Building server artifact..."
    & $buildScript -SkipTests
}

if (!(Test-Path $jarPath)) {
    throw "Server jar not found: $jarPath"
}

if ($KeyPath -ne "" -and !(Test-Path $KeyPath)) {
    throw "SSH key not found: $KeyPath"
}

Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

$assetRoot = $PSScriptRoot
Copy-Item -Force (Join-Path $assetRoot "provision-server.sh") (Join-Path $tmpDir "provision-server.sh")
Copy-Item -Force (Join-Path $assetRoot "wifichat-server.service") (Join-Path $tmpDir "wifichat-server.service")
Copy-Item -Force (Join-Path $assetRoot "wifichat-backup.sh") (Join-Path $tmpDir "wifichat-backup.sh")
Copy-Item -Force (Join-Path $assetRoot "wifichat-backup.service") (Join-Path $tmpDir "wifichat-backup.service")
Copy-Item -Force (Join-Path $assetRoot "wifichat-backup.timer") (Join-Path $tmpDir "wifichat-backup.timer")
Copy-Item -Force $jarPath (Join-Path $tmpDir "wifi-chat-server.jar")

$setupFirewallInt = if ($SetupFirewall) { 1 } else { 0 }
$openUdpInt = if ($OpenUdpDiscovery) { 1 } else { 0 }

$deployEnv = @"
APP_PORT=$AppPort
DB_PATH=$(New-BashLiteral $DbPath)
REMOTE_DIR=$(New-BashLiteral $RemoteDir)
SETUP_FIREWALL=$setupFirewallInt
OPEN_UDP_DISCOVERY=$openUdpInt
UDP_PORT=$UdpPort
"@
Set-Content -Path (Join-Path $tmpDir "deploy.env") -Value $deployEnv -Encoding ASCII

$sshArgs = @("-p", $Port.ToString(), "-o", "StrictHostKeyChecking=accept-new")
if ($KeyPath -ne "") {
    $sshArgs += @("-i", $KeyPath)
}

$scpArgs = @("-P", $Port.ToString(), "-o", "StrictHostKeyChecking=accept-new")
if ($KeyPath -ne "") {
    $scpArgs += @("-i", $KeyPath)
}

$remoteDeployDir = "/tmp/wifichat-deploy"
$sshTarget = "$User@$ServerIp"

Write-Host "Preparing remote temp directory..."
& ssh @sshArgs $sshTarget "rm -rf $remoteDeployDir && mkdir -p $remoteDeployDir"
if ($LASTEXITCODE -ne 0) {
    throw "Unable to prepare remote directory."
}

Write-Host "Uploading deployment assets..."
$assetFiles = @(
    "provision-server.sh",
    "wifichat-server.service",
    "wifichat-backup.sh",
    "wifichat-backup.service",
    "wifichat-backup.timer",
    "wifi-chat-server.jar",
    "deploy.env"
)

foreach ($asset in $assetFiles) {
    $localAsset = Join-Path $tmpDir $asset
    & scp @scpArgs $localAsset "$sshTarget`:$remoteDeployDir/$asset"
    if ($LASTEXITCODE -ne 0) {
        throw "Upload failed for $asset."
    }
}

$remoteCmd = "set -a; source $remoteDeployDir/deploy.env; set +a; bash $remoteDeployDir/provision-server.sh --deploy-dir $remoteDeployDir"
Write-Host "Running remote provisioning..."
& ssh @sshArgs $sshTarget $remoteCmd
if ($LASTEXITCODE -ne 0) {
    throw "Remote provisioning failed."
}

Write-Host ""
Write-Host "Deployment completed."
Write-Host "Check service status:"
Write-Host "  ssh -p $Port $sshTarget 'systemctl status wifichat-server --no-pager'"
