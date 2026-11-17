<#
  build.ps1 - DG J-Code
  jar -> cek isi jar -> runtime mini (jlink) -> UJI 1 -> app-image (.exe) -> UJI 2 -> installer

  Contoh:
    .\build.ps1 -Type app-image                 # folder portable + zip, tanpa WiX
    .\build.ps1 -Version 1.0.1                  # MSI (versi harus naik setiap rilis)
    .\build.ps1 -Type all -Version 1.0.1        # zip portable + MSI + Setup.exe
    .\build.ps1 -Type app-image -Console        # exe membuka jendela konsol untuk melihat error
    .\build.ps1 -FullRuntime                    # runtime berisi SEMUA modul JDK (lebih besar, paling aman)
#>
param(
    [string]$Version = "1.0.0",
    [ValidateSet("msi", "exe", "app-image", "all")]
    [string]$Type = "msi",
    [switch]$SkipMaven,
    [switch]$SkipTest,
    [switch]$FullRuntime,
    [switch]$Console
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# ---------------- pengaturan proyek (sesuaikan) ----------------
$AppName     = "DG J-Code"
$Vendor      = "DG"
$Description = "Editor Java untuk belajar dan praktek"
$MainClass   = "id.dg.jcode.Main"
$JarName     = "dg-jcode.jar"
$IconIco     = "assets\dgjcode.ico"
$LogoPng     = "assets\logo.png"
$AssetBase   = "DG-J-Code"                     # nama file rilis TANPA spasi
$UpgradeUuid = "e3b0c442-989b-4649-b98e-289b4b0e5153"     # GUID yang SAMA seperti sebelumnya, jangan diganti

$Build    = Join-Path $PSScriptRoot "build"
$Dist     = Join-Path $PSScriptRoot "dist"
$InputDir = Join-Path $Build "input"
$Runtime  = Join-Path $Build "runtime"
$ImageDir = Join-Path $Build "image"
$AppImage = Join-Path $ImageDir $AppName

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Ok($msg)   { Write-Host "    OK  $msg" -ForegroundColor Green }
function Run($exe, [string[]]$argv) {
    $old = $ErrorActionPreference
    $ErrorActionPreference = "Continue"   # alat Java menulis peringatan ke stderr; itu bukan kegagalan
    try { & $exe @argv } finally { $ErrorActionPreference = $old }
    if ($LASTEXITCODE -ne 0) { throw "Gagal (kode $LASTEXITCODE): $exe $($argv -join ' ')" }
}
function Show-Error($title, $text) {
    Write-Host "`n----- $title -----" -ForegroundColor Red
    Write-Host $text
    Write-Host "------------------------------" -ForegroundColor Red
}

# ---------------- 0. cek alat ----------------
Step "Memeriksa alat"
if (-not $env:JAVA_HOME) { throw "JAVA_HOME belum diatur. Arahkan ke folder JDK." }
$Bin      = Join-Path $env:JAVA_HOME "bin"
$Java     = Join-Path $Bin "java.exe"
$Jdeps    = Join-Path $Bin "jdeps.exe"
$Jlink    = Join-Path $Bin "jlink.exe"
$Jpackage = Join-Path $Bin "jpackage.exe"
foreach ($t in @($Java, $Jdeps, $Jlink, $Jpackage)) {
    if (-not (Test-Path $t)) { throw "Tidak ditemukan: $t (JAVA_HOME harus JDK, bukan JRE)." }
}
$releaseText = Get-Content (Join-Path $env:JAVA_HOME "release") -Raw
if ($releaseText -notmatch 'JAVA_VERSION="(\d+)') { throw "Versi JDK tidak terbaca." }
$JavaFeature = [int]$Matches[1]
if ($JavaFeature -lt 17) { throw "Butuh JDK 17 atau lebih baru (sekarang JDK $JavaFeature)." }
Ok "JDK $JavaFeature di $env:JAVA_HOME"

if ($Version -notmatch '^\d{1,3}\.\d{1,3}\.\d{1,5}$') { throw "Versi harus seperti 1.0.1 (aturan MSI)." }

$Installers = @(switch ($Type) { "all" { "msi"; "exe" } "app-image" { } default { $Type } })
if ($Installers.Count -gt 0) {
    $tmp = [guid]::Empty
    if (-not [guid]::TryParse($UpgradeUuid, [ref]$tmp)) { throw "Isi `$UpgradeUuid dengan GUID milikmu." }
    $hasWix = (Get-Command candle.exe -ErrorAction SilentlyContinue) -or (Get-Command wix.exe -ErrorAction SilentlyContinue)
    if (-not $hasWix) {
        $wixBin = Get-Item "${env:ProgramFiles(x86)}\WiX Toolset v3*\bin", "$env:ProgramFiles\WiX Toolset v3*\bin" `
                  -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($wixBin) { $env:Path = "$($wixBin.FullName);$env:Path" }
        else { throw "WiX Toolset tidak ditemukan. Pasang dulu: winget install --id WiXToolset.WiXToolset -e" }
    }
    Ok "WiX tersedia"
}

# ---------------- 1. ikon ----------------
if (-not (Test-Path $IconIco)) {
    if ((Test-Path $LogoPng) -and (Get-Command magick -ErrorAction SilentlyContinue)) {
        Step "Membuat $IconIco dari $LogoPng"
        Run "magick" @($LogoPng, "-resize", "256x256", "-background", "none", "-gravity", "center",
                       "-extent", "256x256", "-define", "icon:auto-resize=256,128,64,48,32,24,16", $IconIco)
    } else {
        throw "Ikon $IconIco belum ada. Buat dengan ImageMagick atau taruh file .ico di sana."
    }
}
$IconFull = (Resolve-Path $IconIco).Path

# ---------------- 2. build jar ----------------
if (-not $SkipMaven) {
    Step "mvn clean package"
    Run "mvn" @("-B", "clean", "package")
}
$JarPath = Join-Path $PSScriptRoot "target\$JarName"
if (-not (Test-Path $JarPath)) { throw "target\$JarName tidak ada. Cek <finalName> di pom.xml." }

# ---------------- 3. periksa isi jar ----------------
Step "Memeriksa isi $JarName"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$names = @{}
$manifest = ""
$zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
try {
    foreach ($e in $zip.Entries) { $names[$e.FullName] = $true }
    $me = $zip.GetEntry("META-INF/MANIFEST.MF")
    if ($me) { $r = New-Object System.IO.StreamReader($me.Open()); $manifest = $r.ReadToEnd(); $r.Close() }
} finally { $zip.Dispose() }

$mainEntry = ($MainClass -replace '\.', '/') + ".class"
if (-not $names.ContainsKey($mainEntry)) { throw "Class $MainClass tidak ada di dalam jar. Cek `$MainClass." }

$required = [ordered]@{
    "FlatLaf"         = "com/formdev/flatlaf/FlatLaf.class"
    "RSyntaxTextArea" = "org/fife/ui/rsyntaxtextarea/RSyntaxTextArea.class"
    "PDFBox"          = "org/apache/pdfbox/pdmodel/PDDocument.class"
}
$missing = @($required.Keys | Where-Object { -not $names.ContainsKey($required[$_]) })
$UseLibDir = $false
if ($missing.Count -gt 0) {
    if ((Test-Path "target\lib\*.jar") -and ($manifest -match 'Class-Path:')) {
        Write-Warning "Library ($($missing -join ', ')) tidak ada di dalam jar; folder target\lib ikut dikemas."
        $UseLibDir = $true
    } else {
        throw ("Jar TIDAK berisi library: $($missing -join ', ').`n" +
               "Inilah penyebab DG J-Code.exe tidak terbuka. Pasang maven-shade-plugin (Langkah 3), lalu build ulang.")
    }
}
$signed = @($names.Keys | Where-Object { $_ -match '^META-INF/[^/]+\.(SF|RSA|DSA|EC)$' })
if ($signed.Count -gt 0) {
    throw "Jar berisi file tanda tangan ($($signed -join ', ')) -> 'Invalid signature file digest'. Tambahkan filter di maven-shade-plugin."
}
Ok "jar lengkap$(if ($UseLibDir) { ' (memakai folder lib)' })"

Remove-Item $Build, $Dist -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $InputDir, $ImageDir, $Dist -Force | Out-Null
Copy-Item $JarPath $InputDir
if ($UseLibDir) { Copy-Item "target\lib" (Join-Path $InputDir "lib") -Recurse }

# ---------------- 4. modul runtime ----------------
Step "Menentukan modul runtime"
$available = @(& $Java --list-modules | ForEach-Object { ($_ -split '@')[0].Trim() } | Where-Object { $_ })
if ($FullRuntime) {
    $mods = $available
} else {
    $jars = @(Get-ChildItem $InputDir -Filter *.jar -Recurse | ForEach-Object { $_.FullName })
    $old = $ErrorActionPreference; $ErrorActionPreference = "Continue"
    $jd = & $Jdeps --print-module-deps --ignore-missing-deps --multi-release $JavaFeature @jars
    $jdOk = ($LASTEXITCODE -eq 0)
    $ErrorActionPreference = $old
    $jdMods = @()
    if ($jdOk -and $jd) {
        $line = @($jd | Where-Object { $_ -match '^[\w.]+(,[\w.]+)*$' } | Select-Object -Last 1)
        if ($line.Count -gt 0) { $jdMods = $line[0] -split ',' }
    }
    if ($jdMods.Count -eq 0) { Write-Warning "jdeps gagal; memakai daftar modul aman saja." }
    # Daftar aman: modul yang sering dipakai library secara tidak langsung (refleksi, service loader),
    # plus jdk.compiler untuk javac di memori (koreksi kode & Run) yang tidak terdeteksi jdeps.
    $safe = @("java.base", "java.compiler", "java.datatransfer", "java.desktop", "java.instrument",
              "java.logging", "java.management", "java.naming", "java.net.http", "java.prefs",
              "java.scripting", "java.sql", "java.xml", "jdk.accessibility", "jdk.charsets",
              "jdk.compiler", "jdk.localedata", "jdk.management", "jdk.net", "jdk.unsupported", "jdk.zipfs")
    $mods = @(($jdMods + $safe) | Where-Object { $_ -and ($available -contains $_) } | Sort-Object -Unique)
}
$AllMods = $mods -join ","
Ok "$($mods.Count) modul"

# ---------------- 5. jlink ----------------
Step "jlink: membuat runtime Java mini"
$compress = if ($JavaFeature -ge 21) { "--compress=zip-6" } else { "--compress=2" }
$jlinkArgs = @("--add-modules", $AllMods, "--output", $Runtime, "--strip-debug",
               "--no-header-files", "--no-man-pages", $compress)
if ($mods -contains "jdk.localedata") { $jlinkArgs += "--include-locales=en,id" }
# JANGAN pakai --strip-native-commands: tombol Run butuh runtime\bin\java.exe
Run $Jlink $jlinkArgs

$RtJava = Join-Path $Runtime "bin\java.exe"
if (-not (Test-Path $RtJava)) { throw "runtime\bin\java.exe tidak ada." }
$rtMods = @(& $RtJava --list-modules | ForEach-Object { ($_ -split '@')[0].Trim() })
foreach ($m in @("java.desktop", "java.prefs", "jdk.compiler")) {
    if ($rtMods -notcontains $m) { throw "Runtime tidak berisi modul $m." }
}
Ok "runtime siap"

# ---------------- 6. UJI 1: jar + runtime mini di folder kosong ----------------
if (-not $SkipTest) {
    Step "Uji 1: jar + runtime mini di folder kosong (jendela aplikasi muncul lalu ditutup otomatis)"
    $testDir = Join-Path $env:TEMP ("dgjcode-test-" + [guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Path $testDir | Out-Null
    Copy-Item (Join-Path $InputDir "*") $testDir -Recurse
    $outLog = Join-Path $testDir "_stdout.txt"
    $errLog = Join-Path $testDir "_stderr.txt"
    $p = Start-Process -FilePath $RtJava `
         -ArgumentList @("-Dfile.encoding=UTF-8", "-jar", "`"$(Join-Path $testDir $JarName)`"") `
         -WorkingDirectory $testDir -RedirectStandardOutput $outLog -RedirectStandardError $errLog -PassThru
    $null = $p.Handle
    $exited = $p.WaitForExit(15000)
    if (-not $exited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue; Start-Sleep -Milliseconds 700 }
    $errText = if (Test-Path $errLog) { Get-Content $errLog -Raw } else { "" }
    if ($exited -or ($errText -match '(Exception|Error)\b')) {
        Show-Error "pesan error (Uji 1)" $(if ($errText) { $errText } else { "(stderr kosong; program menutup sendiri, kode $($p.ExitCode))" })
        throw "Uji 1 gagal: aplikasi tidak bisa jalan dengan runtime mini. Ini penyebab DG J-Code.exe tidak terbuka. Cocokkan pesan di atas dengan tabel di panduan."
    }
    Remove-Item $testDir -Recurse -Force -ErrorAction SilentlyContinue
    Ok "aplikasi berjalan 15 detik tanpa error"
}

# ---------------- 7. app-image: DG J-Code.exe ----------------
Step "jpackage: membuat app-image ($AppName.exe + runtime + app)"
$imgArgs = @("--type", "app-image", "--name", $AppName, "--app-version", $Version,
             "--vendor", $Vendor, "--description", $Description,
             "--input", $InputDir, "--main-jar", $JarName, "--main-class", $MainClass,
             "--runtime-image", $Runtime, "--icon", $IconFull, "--dest", $ImageDir,
             "--java-options", "-Dfile.encoding=UTF-8")
if ($Console) { $imgArgs += "--win-console" }
Run $Jpackage $imgArgs

$Exe = Join-Path $AppImage "$AppName.exe"
foreach ($f in @($Exe, (Join-Path $AppImage "app\$JarName"), (Join-Path $AppImage "app\$AppName.cfg"))) {
    if (-not (Test-Path $f)) { throw "File hasil jpackage tidak lengkap, tidak ada: $f" }
}
if (-not (Test-Path (Join-Path $AppImage "runtime\bin\java.exe"))) {
    Write-Warning "runtime\bin\java.exe tidak ada di app-image; tombol Run di Praktek tidak akan berfungsi."
}
Ok "app-image: $AppImage"

# ---------------- 8. UJI 2: jalankan exe ----------------
if (-not $SkipTest -and -not $Console) {
    Step "Uji 2: menjalankan $AppName.exe (jendela muncul lalu ditutup otomatis)"
    $logFile = Join-Path $env:LOCALAPPDATA "DGJCode\logs\dgjcode.log"
    $before = if (Test-Path $logFile) { (Get-Item $logFile).LastWriteTime } else { [datetime]::MinValue }
    $p = Start-Process -FilePath $Exe -WorkingDirectory $AppImage -PassThru
    $null = $p.Handle
    $exited = $p.WaitForExit(15000)
    if (-not $exited) { Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue; Start-Sleep -Milliseconds 700 }
    $logText = ""
    if ((Test-Path $logFile) -and (Get-Item $logFile).LastWriteTime -gt $before) { $logText = Get-Content $logFile -Raw }
    if ($exited -or ($logText -match '(Exception|Error)\b')) {
        if ($logText) { Show-Error "isi $logFile" $logText }
        throw "Uji 2 gagal: $AppName.exe menutup sendiri. Jalankan: .\build.ps1 -Type app-image -Console  lalu buka exe-nya untuk melihat error."
    }
    Ok "$AppName.exe berjalan"
}

# ---------------- 9. zip portable ----------------
Step "Membuat zip portable"
Compress-Archive -Path $AppImage -DestinationPath (Join-Path $Dist "$AssetBase-Portable.zip") -Force
Ok "$AssetBase-Portable.zip"

# ---------------- 10. installer dari app-image yang sudah diuji ----------------
foreach ($t in $Installers) {
    Step "jpackage --type $t"
    Run $Jpackage @("--type", $t, "--app-image", $AppImage, "--name", $AppName,
                    "--app-version", $Version, "--vendor", $Vendor, "--description", $Description,
                    "--dest", $Dist,
                    "--win-menu", "--win-menu-group", $AppName,
                    "--win-shortcut", "--win-shortcut-prompt",
                    "--win-dir-chooser", "--win-per-user-install",
                    "--win-upgrade-uuid", $UpgradeUuid)
    $made = Get-ChildItem $Dist -Filter "*.$t" | Where-Object { $_.Name -ne "$AssetBase-Setup.$t" } |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $made) { throw "File .$t tidak ditemukan di $Dist" }
    Move-Item $made.FullName (Join-Path $Dist "$AssetBase-Setup.$t") -Force
    Ok "$AssetBase-Setup.$t"
}

Step "Selesai"
Get-ChildItem $Dist | Format-Table Name, @{ n = "Ukuran (MB)"; e = { "{0:N1}" -f ($_.Length / 1MB) } } -AutoSize
Write-Host "Versi portable (tanpa instal): $AppImage"
