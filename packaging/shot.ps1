#requires -Version 5.1
<#
.SYNOPSIS
  启动 KitBox 指定工具页并对该窗口截图（PrintWindow，无需置顶/前台）。
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File packaging\shot.ps1 default   # 主页
  powershell -ExecutionPolicy Bypass -File packaging\shot.ps1 darkdefault
.NOTES
  产物：packaging\shot-<Key>.png，尺寸即应用窗口大小。
#>
param([string]$Key)
$ErrorActionPreference = "Stop"
$root = (Split-Path -Parent $PSScriptRoot)
$map = @{
    default  = ""
    darkdefault = ""
    json     = "JSON 工具"
    convert  = "转换工具"
    checksum = "文件批量校验"
    keystore = "密钥库"
    settings = "设置"
    darkkey  = "密钥库"
    asym     = "非对称加解密"
    sign     = "加签 / 验签"
    digest   = "摘要与 HMAC"
    jsonfield = "JSON 字段加解密"
    msgfmt   = "报文格式加解密"
    encode   = "编码转换"
    qr       = "二维码工具"
    pwdgen   = "密码生成器"
    fake     = "假数据生成"
    fileb64  = "文件 Base64"
    sym      = "对称加解密"
    keystorelight = "密钥库"
}
$java = Join-Path $root "packaging\dist\KitBox\runtime\bin\java.exe"
$jar = Join-Path $root "target\KitBox.jar"

$jarArgs = New-Object System.Collections.Generic.List[string]
$tool = $map[$Key]
if ($tool -ne "") {
    $jarArgs.Add('"-Dkitbox.tool=' + $tool + '"')
}
if ($Key -eq "darkkey" -or $Key -eq "darkdefault") {
    $jarArgs.Add("-Dkitbox.theme=dark")
}
$jarArgs.Add("-jar")
$jarArgs.Add('"' + $jar + '"')

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win32Shot {
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);
    [DllImport("user32.dll")] public static extern bool PrintWindow(IntPtr hWnd, IntPtr hdc, uint flags);
    [DllImport("dwmapi.dll")] public static extern int DwmGetWindowAttribute(IntPtr hwnd, uint attr, out RECT rect, uint size);
}
"@

# DPI 感知：保证多缩放环境下 GetWindowRect 拿到物理像素
[Win32Shot]::SetProcessDPIAware() | Out-Null

$proc = Start-Process -FilePath $java -ArgumentList $jarArgs -PassThru -WorkingDirectory $root
# 轮询等待主窗口就绪（最长 30 秒）：机器负载高时 JVM 启动可能远超固定等待
$h = [IntPtr]::Zero
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Milliseconds 500
    $proc.Refresh()
    if ($proc.HasExited) { break }
    if ($proc.MainWindowHandle -ne [IntPtr]::Zero) {
        $h = $proc.MainWindowHandle
        break
    }
}
if ($h -eq [IntPtr]::Zero) {
    throw "未找到 KitBox 主窗口（进程可能启动失败或已退出）"
}

# 最小化时恢复显示（不抢前台焦点），再等渲染稳定
if ([Win32Shot]::IsIconic($h)) {
    [Win32Shot]::ShowWindow($h, 4) | Out-Null   # SW_SHOWNOACTIVATE
    Start-Sleep -Milliseconds 800
}
Start-Sleep -Milliseconds 1200

# 窗口尺寸：优先 DWM 扩展框架边界（剔除 Win10/11 不可见的缩放边），失败退回 GetWindowRect
$rect = New-Object Win32Shot+RECT
if ([Win32Shot]::DwmGetWindowAttribute($h, 9, [ref]$rect, 16) -ne 0) {
    [Win32Shot]::GetWindowRect($h, [ref]$rect) | Out-Null
}
$width = $rect.Right - $rect.Left
$height = $rect.Bottom - $rect.Top
if ($width -le 0 -or $height -le 0) {
    throw "窗口尺寸异常（${width}x${height}）"
}

Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap $width, $height
$g = [System.Drawing.Graphics]::FromImage($bmp)
$hdc = $g.GetHdc()
# PW_RENDERFULLCONTENT(2)：窗口被遮挡或不在前台也能截到完整内容
[Win32Shot]::PrintWindow($h, $hdc, 2) | Out-Null
$g.ReleaseHdc($hdc)
$g.Dispose()
$bmp.Save((Join-Path $root ("packaging\shot-" + $Key + ".png")), [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
Write-Output ("saved shot-" + $Key + ".png (窗口 ${width}x${height})")
