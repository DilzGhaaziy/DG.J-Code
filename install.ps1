# install.ps1 - memasang DG J-Code versi terbaru dari GitHub Releases
$ErrorActionPreference = 'Stop'
$Repo  = 'DilzGhaaziy/DG.J-Code'
$Asset = 'DG-J-Code-Setup.msi'
$Url   = "https://github.com/DilzGhaaziy/DG.J-Code/releases/download/DG.J-Code/DG-J-Code-Setup.msi"
$Msi   = Join-Path $env:TEMP $Asset

[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
$ProgressPreference = 'SilentlyContinue'   # unduhan jauh lebih cepat di PowerShell 5.1

Write-Host "Mengunduh DG J-Code... Tunggu yak😊  -   Dilz Ghaaziy" -ForegroundColor Cyan
Invoke-WebRequest -Uri $Url -OutFile $Msi -UseBasicParsing

Write-Host "Memasang..." -ForegroundColor Cyan
$p = Start-Process msiexec.exe -ArgumentList @('/i', "`"$Msi`"", '/passive', '/norestart') -Wait -PassThru
Remove-Item $Msi -ErrorAction SilentlyContinue

switch ($p.ExitCode) {
    0         { 
        Write-Host "Selesai! DG J-Code sudah ada di Desktop & Start Menu.    -Dilz Ghaaziy" -ForegroundColor Green
        Write-Host "dilzghaaziy.my.id" -ForegroundColor Blue
        Write-Host ""
    }
    3010      { Write-Host "Selesai. Restart komputer untuk menyelesaikan instalasi." -ForegroundColor Yellow }
    1602      { Write-Host "Instalasi dibatalkan." -ForegroundColor Yellow }
    default   { throw "Instalasi gagal (kode $($p.ExitCode))." }
}
