#requires -Version 5.1
# 生成 KitBox 应用图标：多尺寸 PNG 嵌入 ICO，输出 packaging/icon/kitbox.ico
# 可随时用自己的 ico 替换该文件后重新打包。
$ErrorActionPreference = "Stop"
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch {}
Add-Type -AssemblyName System.Drawing

$OutPath = Join-Path $PSScriptRoot "icon\kitbox.ico"
$Dir = Split-Path -Parent $OutPath
if (-not (Test-Path $Dir)) { New-Item -ItemType Directory -Path $Dir | Out-Null }

# 同时导出各尺寸 PNG 到应用资源，供 MainWindow.setIconImages 使用（修复任务栏显示 Java 图标）
$ResDir = Join-Path (Split-Path -Parent $PSScriptRoot) "src\main\resources\icon"
if (-not (Test-Path $ResDir)) { New-Item -ItemType Directory -Path $ResDir | Out-Null }

$BG = [System.Drawing.Color]::FromArgb(255, 68, 108, 245)
$FG = [System.Drawing.Color]::White
$sizes = 16, 24, 32, 48, 64, 128, 256
$entries = @()

foreach ($s in $sizes) {
    $bmp = New-Object System.Drawing.Bitmap($s, $s)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.Clear([System.Drawing.Color]::Transparent)

    # 圆角方块背景
    $r = [Math]::Max(2, [int]($s * 0.22))
    $d = $r * 2
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddArc(0, 0, $d, $d, 180, 90)
    $path.AddArc($s - $d - 1, 0, $d, $d, 270, 90)
    $path.AddArc($s - $d - 1, $s - $d - 1, $d, $d, 0, 90)
    $path.AddArc(0, $s - $d - 1, $d, $d, 90, 90)
    $path.CloseFigure()
    $brush = New-Object System.Drawing.SolidBrush($BG)
    $g.FillPath($brush, $path)

    # 字母 K
    $font = New-Object System.Drawing.Font("Segoe UI", [float]($s * 0.62), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $fgBrush = New-Object System.Drawing.SolidBrush($FG)
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::Center
    $fmt.LineAlignment = [System.Drawing.StringAlignment]::Center
    $rect = New-Object System.Drawing.RectangleF([float]0, [float]($s * 0.02), [float]$s, [float]$s)
    $g.DrawString("K", $font, $fgBrush, $rect, $fmt)

    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Save((Join-Path $ResDir ("icon{0}.png" -f $s)), [System.Drawing.Imaging.ImageFormat]::Png)
    $dim = $(if ($s -ge 256) { [byte]0 } else { [byte]$s })
    $entries += ,@($dim, $dim, $ms.ToArray())

    $fmt.Dispose(); $fgBrush.Dispose(); $font.Dispose(); $brush.Dispose()
    $path.Dispose(); $g.Dispose(); $bmp.Dispose(); $ms.Dispose()
}

$fs = [System.IO.File]::Create($OutPath)
$bw = New-Object System.IO.BinaryWriter($fs)
$bw.Write([uint16]0); $bw.Write([uint16]1); $bw.Write([uint16]$entries.Count)
$offset = 6 + 16 * $entries.Count
foreach ($e in $entries) {
    $bw.Write([byte]$e[0]); $bw.Write([byte]$e[1])
    $bw.Write([byte]0); $bw.Write([byte]0)
    $bw.Write([uint16]1); $bw.Write([uint16]32)
    $bw.Write([uint32]$e[2].Length)
    $bw.Write([uint32]$offset)
    $offset += $e[2].Length
}
foreach ($e in $entries) { $bw.Write([byte[]]$e[2]) }
$bw.Flush(); $bw.Close(); $fs.Dispose()
Write-Host ("OK icon -> {0} ({1} bytes)" -f $OutPath, (Get-Item $OutPath).Length)
