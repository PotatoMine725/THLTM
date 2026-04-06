param(
    [int]$Port = 61000,
    [string]$DbPath = "",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $root "out\wifi-chat-server.jar"

function Test-JarContains {
    param(
        [string]$Jar,
        [string]$EntryPath
    )

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Jar)
    try {
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName -eq $EntryPath) {
                return $true
            }
        }
        return $false
    }
    finally {
        $zip.Dispose()
    }
}

if (-not $SkipBuild) {
    Write-Host "Running build before launch server..."
    & (Join-Path $PSScriptRoot "build.ps1") -SkipTests
}

if (!(Test-Path $jarPath)) {
    throw "Server jar not found: $jarPath"
}

if (-not (Test-JarContains -Jar $jarPath -EntryPath "org/sqlite/JDBC.class")) {
    throw "Server jar does not contain sqlite-jdbc. Build with Maven (not javac fallback) and run again."
}

$args = @("--port", $Port)
if ($DbPath -ne "") {
    $args += "--db"
    $args += $DbPath
}

java -jar $jarPath @args
