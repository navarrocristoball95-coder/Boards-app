Add-Type -AssemblyName System.Drawing

$sourcePath = "C:\Users\Cristóbal\.gemini\antigravity\brain\98bba9b3-82da-417a-9310-61f18caa0bef\app_launcher_icon_lightning_1786995733891.jpg"
$srcImg = [System.Drawing.Image]::FromFile($sourcePath)

$resDir = "C:\Users\Cristóbal\.gemini\antigravity\scratch\QuickReplyBoards\app\src\main\res"
$sizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

foreach ($folder in $sizes.Keys) {
    $dim = $sizes[$folder]
    $targetDir = Join-Path $resDir $folder
    if (-not (Test-Path $targetDir)) { 
        New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    }
    
    $bmp = New-Object System.Drawing.Bitmap $dim, $dim
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.DrawImage($srcImg, 0, 0, $dim, $dim)
    $g.Dispose()

    $outSquare = Join-Path $targetDir "ic_launcher.png"
    $outRound = Join-Path $targetDir "ic_launcher_round.png"
    $bmp.Save($outSquare, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Save($outRound, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "Generated $folder (${dim}x${dim})"
}

# High-res drawable icon
$highResDir = Join-Path $resDir "drawable"
$bmpHigh = New-Object System.Drawing.Bitmap 512, 512
$gHigh = [System.Drawing.Graphics]::FromImage($bmpHigh)
$gHigh.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gHigh.DrawImage($srcImg, 0, 0, 512, 512)
$gHigh.Dispose()
$bmpHigh.Save((Join-Path $highResDir "app_icon.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$bmpHigh.Dispose()

# Web public icon
$webPublic = "C:\Users\Cristóbal\.gemini\antigravity\scratch\QuickReplyBoards\boards-web\public"
if (Test-Path $webPublic) {
    $srcImg.Save((Join-Path $webPublic "icon.png"), [System.Drawing.Imaging.ImageFormat]::Png)
}

$srcImg.Dispose()
Write-Host "All launcher icons generated successfully!"
