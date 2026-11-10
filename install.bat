@echo off
setlocal
title Pemasang DG J-Code

:: Konfigurasi Repository dan URL Unduhan
set "REPO=DilzGhaaziy/DG.J-Code"
set "ASSET=DG-J-Code-Setup.msi"
set "URL=https://github.com/DilzGhaaziy/DG.J-Code/releases/download/DG.J-Code/DG-J-Code-Setup.msi"
set "MSI_PATH=%TEMP%\%ASSET%"

echo [1/3] Mengunduh DG J-Code...
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('%URL%', '%MSI_PATH%')"

if not exist "%MSI_PATH%" (
    echo [ERROR] Gagal mengunduh instalasi dari GitHub. Periksa koneksi internet Anda.
    pause
    exit /b 1
)

echo [2/3] Memasang DG J-Code...
start /wait msiexec.exe /i "%MSI_PATH%" /passive /norestart
set "EXIT_CODE=%ERRORLEVEL%"

:: Bersihkan file installer temporer
if exist "%MSI_PATH%" del /q "%MSI_PATH%"

echo [3/3] Menyelesaikan...
if %EXIT_CODE% EQU 0 (
    echo Selesai! Buka DG J-Code dari Start Menu.
) else if %EXIT_CODE% EQU 3010 (
    echo Selesai. Restart komputer Anda untuk menyelesaikan instalasi.
) else if %EXIT_CODE% EQU 1602 (
    echo Instalasi dibatalkan oleh pengguna.
) else (
    echo [ERROR] Instalasi gagal dengan kode keluar: %EXIT_CODE%
)

pause
endlocal
