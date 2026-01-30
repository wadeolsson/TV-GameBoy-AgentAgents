param(
    [string]$Gradle = ".\gradlew.bat",
    [string]$Project = ":launcher",
    [int]$DebounceSeconds = 1
)

$lastChange = [datetime]::MinValue
$process = $null

function Build-And-Start {
    if ($process -and -not $process.HasExited) {
        Write-Host "Stopping process PID $($process.Id)..."
        try { $process.Kill(); $process.WaitForExit() } catch {}
        $process = $null
    }

    Write-Host "Running $Gradle $Project:installDist ..."
    & $Gradle "$Project:installDist"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build/installDist failed. Waiting for next change..."
        return
    }

    $bin = Join-Path -Path $PSScriptRoot -ChildPath "launcher\build\install\launcher\bin\launcher.bat"
    if (-not (Test-Path $bin)) {
        Write-Host "Launcher script not found at $bin"
        return
    }

    Write-Host "Starting launcher (via installDist script)..."
    $process = Start-Process -FilePath $bin -NoNewWindow -PassThru
}

$fsw = New-Object System.IO.FileSystemWatcher
$fsw.Path = (Get-Location).Path
$fsw.IncludeSubdirectories = $true
$fsw.Filter = "*.java"
$fsw.EnableRaisingEvents = $true

$onChange = {
    $global:lastChange = Get-Date
}

Register-ObjectEvent $fsw Changed -Action $onChange | Out-Null
Register-ObjectEvent $fsw Created -Action $onChange | Out-Null
Register-ObjectEvent $fsw Deleted -Action $onChange | Out-Null
Register-ObjectEvent $fsw Renamed -Action $onChange | Out-Null

Write-Host "Watcher started. Building and launching $Project..."
Build-And-Start

while ($true) {
    Start-Sleep -Milliseconds 500
    if ($lastChange -ne [datetime]::MinValue) {
        $elapsed = (Get-Date) - $lastChange
        if ($elapsed.TotalSeconds -ge $DebounceSeconds) {
            Write-Host "Detected changes. Rebuilding and restarting..."
            Build-And-Start
            $lastChange = [datetime]::MinValue
        }
    }
}
