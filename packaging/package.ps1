#requires -Version 5.1
<#
.SYNOPSIS
  KitBox 一键打包：构建 fat jar -> jlink 精简运行时 -> jpackage 免安装应用镜像 -> zip
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File packaging\package.ps1                     # 构建并跑测试
  powershell -ExecutionPolicy Bypass -File packaging\package.ps1 -SkipTests          # 跳过测试
  powershell -ExecutionPolicy Bypass -File packaging\package.ps1 -Jdk "D:\Program Files\java\jdk-21.0.3"
.NOTES
  产物在 packaging\dist\：KitBox\（免安装目录）与 KitBox-<版本>-win-x64.zip。
  脚本需要 JDK 14+（用于 jlink/jpackage），应用仍以 Java 8 字节码运行。
#>
param(
    [string]$Jdk = "",
    [switch]$SkipTests,
    [switch]$NoZip
)

$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$DistDir     = Join-Path $PSScriptRoot "dist"
$StagingDir  = Join-Path $PSScriptRoot "staging"
$IconPath    = Join-Path $PSScriptRoot "icon\kitbox.ico"

function Find-Jdk {
    param([string]$Explicit)
    $roots = @()
    if ($Explicit)       { $roots += $Explicit }
    if ($env:KITBOX_JDK) { $roots += $env:KITBOX_JDK }
    if ($env:JAVA_HOME)  { $roots += $env:JAVA_HOME }
    foreach ($base in @("C:\Program Files\Java", "C:\Program Files\Eclipse Adoptium",
                        "C:\Program Files\Amazon Corretto", "C:\Program Files\Zulu",
                        "C:\Program Files\BellSoft", "D:\Program Files\java", "E:\Program Files\java")) {
        if (Test-Path $base) {
            Get-ChildItem $base -Directory | ForEach-Object { $roots += $_.FullName }
        }
    }
    $found = @()
    foreach ($r in $roots) {
        if (-not $r) { continue }
        if (-not (Test-Path (Join-Path $r "bin\jpackage.exe"))) { continue }
        if (-not (Test-Path (Join-Path $r "bin\jlink.exe")))    { continue }
        $major = 0
        $rel = Join-Path $r "release"
        if (Test-Path $rel -PathType Leaf) {
            $txt = Get-Content $rel -Raw
            if ($txt -match 'JAVA_VERSION="(\d+)') { $major = [int]$Matches[1] }
        }
        if ($major -ge 14) { $found += [pscustomobject]@{ Path = $r; Major = $major } }
    }
    if ($found.Count -eq 0) { return $null }
    # 优先 LTS（17/21，库兼容性最稳），同级别取更高版本
    foreach ($f in $found) {
        $weight = $(if ($f.Major -in @(17, 21)) { 0 } else { 1 })
        $f | Add-Member -NotePropertyName Weight -NotePropertyValue $weight
    }
    return ($found | Sort-Object Weight, @{ Expression = { $_.Major }; Descending = $true } | Select-Object -First 1)
}

Write-Host "==> [1/5] 定位 JDK 14+（jlink/jpackage 需要，优先 LTS）"
# 注意：参数 $Jdk 与局部变量不区分大小写同名会相互覆盖，故用 $jdkInfo
$jdkInfo = Find-Jdk -Explicit $Jdk
if (-not $jdkInfo) {
    throw "未找到带 jpackage 的 JDK 14+。请用 -Jdk 参数指定路径，或设置环境变量 KITBOX_JDK。"
}
$jdkPath = [string]$jdkInfo.Path
$jdkMajor = [int]$jdkInfo.Major
Write-Host ("    JDK: {0}  (JDK {1})" -f $jdkPath, $jdkMajor)

Write-Host "==> [2/5] Maven 构建 fat jar（$(@{ $true='跳过测试'; $false='含测试' }[[bool]$SkipTests])）"
Push-Location $ProjectRoot
try {
    if ($SkipTests) { mvn -DskipTests package } else { mvn package }
    if ($LASTEXITCODE -ne 0) { throw "Maven 构建失败（exit $LASTEXITCODE）" }
} finally { Pop-Location }
$jarPath = Join-Path $ProjectRoot "target\KitBox.jar"
if (-not (Test-Path $jarPath)) { throw "未找到 $jarPath" }

# 版本号取自 pom.xml（artifactId 为 kitbox 的那段）
$version = "1.0.0"
$pomText = Get-Content (Join-Path $ProjectRoot "pom.xml") -Raw
if ($pomText -match '<artifactId>kitbox</artifactId>\s*<version>([^<]+)</version>') {
    $version = $Matches[1]
}
Write-Host "    版本: $version  jar: $jarPath"

Write-Host "==> [3/5] jlink 裁剪精简运行时"
$runtimeDir = Join-Path $DistDir "runtime"
if (Test-Path $runtimeDir) { Remove-Item $runtimeDir -Recurse -Force }
# JDK 21+ 用 zip-6；旧版 jlink 只认 0/1/2（23+ 已移除）
$compress = $(if ($jdkMajor -ge 21) { "zip-6" } else { "2" })
& (Join-Path $jdkPath "bin\jlink.exe") `
    --add-modules "java.base,java.desktop,java.logging,java.xml,jdk.crypto.ec,jdk.charsets" `
    --output $runtimeDir --no-header-files --no-man-pages --strip-debug --compress $compress
if ($LASTEXITCODE -ne 0) { throw "jlink 失败（exit $LASTEXITCODE）" }

Write-Host "==> [4/5] jpackage 生成免安装应用镜像"
if (Test-Path $StagingDir) { Remove-Item $StagingDir -Recurse -Force }
New-Item -ItemType Directory -Path $StagingDir | Out-Null
Copy-Item $jarPath $StagingDir
$appOut = Join-Path $DistDir "KitBox"
if (Test-Path $appOut) { Remove-Item $appOut -Recurse -Force }
$appArgs = @(
    "--type", "app-image",
    "--name", "KitBox",
    "--app-version", $version,
    "--description", "KitBox 工具箱（离线加解密/二维码等本地工具）",
    "--input", $StagingDir,
    "--main-jar", "KitBox.jar",
    "--main-class", "com.kitbox.KitBoxApp",
    "--runtime-image", $runtimeDir,
    "--java-options", "-Dfile.encoding=UTF-8",
    "--dest", $DistDir
)
if (Test-Path $IconPath) {
    $appArgs += @("--icon", $IconPath)
} else {
    Write-Host "    （未找到 $IconPath，使用默认图标；可先运行 packaging\make-icon.ps1）"
}
& (Join-Path $jdkPath "bin\jpackage.exe") @appArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage 失败（exit $LASTEXITCODE）" }

$zipPath = Join-Path $DistDir ("KitBox-{0}-win-x64.zip" -f $version)
if (-not $NoZip) {
    Write-Host "==> [5/5] 压缩 zip"
    if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
    $tar = Join-Path $env:SystemRoot "System32\tar.exe"
    Push-Location $DistDir
    try {
        if (Test-Path $tar) {
            & $tar -a -c -f $zipPath "KitBox" | Out-Null
            if ($LASTEXITCODE -ne 0) { throw "tar 压缩失败（exit $LASTEXITCODE）" }
        } else {
            Compress-Archive -Path $appOut -DestinationPath $zipPath -Force
        }
    } finally { Pop-Location }
} else {
    Write-Host "==> [5/5] 跳过 zip（-NoZip）"
}

Write-Host ""
Write-Host "==================== 打包完成 ===================="
$appSize = (Get-ChildItem $appOut -Recurse | Measure-Object Length -Sum).Sum
Write-Host ("  免安装目录 : {0}  ({1:N1} MB)" -f $appOut, ($appSize / 1MB))
if (-not $NoZip -and (Test-Path $zipPath)) {
    Write-Host ("  分发包     : {0}  ({1:N1} MB)" -f $zipPath, ((Get-Item $zipPath).Length / 1MB))
}
Write-Host "  使用方式   : 解压后双击 KitBox\KitBox.exe，无需安装 Java"
Write-Host "=================================================="
