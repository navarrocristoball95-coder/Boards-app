Add-Type -AssemblyName System.Drawing

$srcImg = [System.Drawing.Image]::FromFile((Resolve-Path "icon_source.jpg").Path)

$resDir = (Resolve-Path "app\src\main\res").Path
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
$bmpHigh.Save((Join-Path $highResDir "ic_launcher_foreground.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$bmpHigh.Dispose()

# Web public icon
$webPublic = (Resolve-Path "boards-web\public").Path
if (Test-Path $webPublic) {
    $srcImg.Save((Join-Path $webPublic "icon.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $srcImg.Save((Join-Path $webPublic "favicon.ico"), [System.Drawing.Imaging.ImageFormat]::Icon)
}

$srcImg.Dispose()
Write-Host "All launcher icons generated successfully!"
