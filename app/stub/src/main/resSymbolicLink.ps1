if (-not ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Start-Process PowerShell.exe -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$($MyInvocation.MyCommand.Path)`"" -Verb RunAs
    exit
}

$scriptPath = $PSScriptRoot
$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $scriptPath)))

$targetPath = Join-Path $repoRoot "app\src\main\res"
$linkPath = Join-Path $repoRoot "app\stub\src\main\res"

Write-Host "Target path: $targetPath"
Write-Host "Link path: $linkPath"

if (Test-Path $linkPath) {
    Remove-Item $linkPath -Force
    Write-Host "Removed existing link/directory at $linkPath"
}

New-Item -ItemType SymbolicLink -Path $linkPath -Target $targetPath
Write-Host "Created symbolic link from $linkPath to $targetPath"

Read-Host "Press Enter to exit"