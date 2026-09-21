param([string]$Key)
$ErrorActionPreference = "Stop"
$root = (Split-Path -Parent $PSScriptRoot)
$map = @{
    default  = ""
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
    keystorelight = "密钥库"
}
$java = Join-Path $root "packaging\dist\KitBox\runtime\bin\java.exe"
$jar = Join-Path $root "target\KitBox.jar"

$jarArgs = New-Object System.Collections.Generic.List[string]
$tool = $map[$Key]
if ($tool -ne "") {
    $jarArgs.Add('"-Dkitbox.tool=' + $tool + '"')
}
if ($Key -eq "darkkey") {
    $jarArgs.Add("-Dkitbox.theme=dark")
}
$jarArgs.Add("-jar")
$jarArgs.Add('"' + $jar + '"')

Add-Type @"
using System;
using System.Runtime.InteropServices;
public class Win32Shot {
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr hWnd, IntPtr after, int x, int y, int cx, int cy, uint flags);
}
"@

$proc = Start-Process -FilePath $java -ArgumentList $jarArgs -PassThru -WorkingDirectory $root
Start-Sleep -Seconds 6
$proc.Refresh()
$h = $proc.MainWindowHandle
if ($h -ne [IntPtr]::Zero) {
    [Win32Shot]::ShowWindow($h, 9) | Out-Null
    [Win32Shot]::SetForegroundWindow($h) | Out-Null
    # HWND_TOPMOST，避免窗口被前台应用遮挡
    [Win32Shot]::SetWindowPos($h, [IntPtr](-1), 0, 0, 0, 0, 0x0003) | Out-Null
}
Start-Sleep -Milliseconds 1200

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
$b = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
$bmp = New-Object System.Drawing.Bitmap $b.Width, $b.Height
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen(0, 0, 0, 0, $bmp.Size)
$bmp.Save((Join-Path $root ("packaging\shot-" + $Key + ".png")), [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Stop-Process -Id $proc.Id -Force
Write-Output ("saved shot-" + $Key + ".png")
