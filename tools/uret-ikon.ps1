# Faz 11 - uygulama ikonu: API 24/25 icin yalli (raster) ikon uretimi.
#
# Adaptive icon yalnizca API 26+ gecerlidir; minSdk 24 oldugu icin
# mipmap-mdpi..xxxhdpi altindaki webp dosyalari hala Android Studio
# sablonunun yesil robotunu gosteriyordu. Asagidaki geometri,
# drawable/ic_launcher_foreground.xml + ic_launcher_background.xml ile
# BIREBIR aynidir; boylece iki kaynak bir gun birbirinden sasmaz.
#
# Calistirma: powershell -File tools\uret-ikon.ps1

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$kok = Split-Path -Parent $PSScriptRoot
$res = Join-Path $kok 'app\src\main\res'

# --- Geometri (108x108 gorunum alani) -------------------------------------
$ZEMIN_UST = [System.Drawing.Color]::FromArgb(255, 0x3A, 0x6B, 0xC0)
$ZEMIN_ALT = [System.Drawing.Color]::FromArgb(255, 0x24, 0x47, 0x8E)
$KART      = [System.Drawing.RectangleF]::new(25, 33, 58, 42)
$KART_YARI = 10
$ISARET_RENK = [System.Drawing.Color]::FromArgb(255, 0x2E, 0x5A, 0xAC)
$ISARET_KALINLIK = 8
$ISARET_NOKTALAR = @(
    [System.Drawing.PointF]::new(34, 54),
    [System.Drawing.PointF]::new(46, 66),
    [System.Drawing.PointF]::new(73, 43)
)

$BOYUTLAR = @{
    'mdpi' = 48; 'hdpi' = 72; 'xhdpi' = 96; 'xxhdpi' = 144; 'xxxhdpi' = 192
}
$UST_BOYUT = 1080   # once buyuk ciz, sonra kucult: sinirlama daha temiz olur

function Add-YuvarlakDikdortgen {
    param($yol, [float]$x, [float]$y, [float]$g, [float]$y2, [float]$r)
    $yol.AddArc($x, $y, 2 * $r, 2 * $r, 180, 90)
    $yol.AddArc($x + $g - 2 * $r, $y, 2 * $r, 2 * $r, 270, 90)
    $yol.AddArc($x + $g - 2 * $r, $y + $y2 - 2 * $r, 2 * $r, 2 * $r, 0, 90)
    $yol.AddArc($x, $y + $y2 - 2 * $r, 2 * $r, 2 * $r, 90, 90)
    $yol.CloseFigure()
}

function Ust-Ciz {
    param([bool]$Yuvarlak)

    $bmp = New-Object System.Drawing.Bitmap($UST_BOYUT, $UST_BOYUT)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.InterpolationMode = 'HighQualityBicubic'
    $g.PixelOffsetMode = 'HighQuality'

    $k = $UST_BOYUT / 108.0

    if ($Yuvarlak) {
        $maske = New-Object System.Drawing.Drawing2D.GraphicsPath
        $e = $UST_BOYUT - 1
        $maske.AddEllipse(0.0, 0.0, $e, $e)
        $g.SetClip($maske)
    }

    # 1) Zemin: dikey tonlama
    $alan = New-Object System.Drawing.Rectangle(0, 0, $UST_BOYUT, $UST_BOYUT)
    $fircasi = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $alan, $ZEMIN_UST, $ZEMIN_ALT, 90)
    $g.FillRectangle($fircasi, $alan)

    # 2) Beyaz odev karti
    $kartYolu = New-Object System.Drawing.Drawing2D.GraphicsPath
    Add-YuvarlakDikdortgen $kartYolu `
        ($KART.X * $k) ($KART.Y * $k) ($KART.Width * $k) ($KART.Height * $k) ($KART_YARI * $k)
    $g.FillPath([System.Drawing.Brushes]::White, $kartYolu)

    # 3) Onay isareti
    $isaretYolu = New-Object System.Drawing.Drawing2D.GraphicsPath
    $noktalar = [System.Drawing.PointF[]]@($ISARET_NOKTALAR | ForEach-Object {
        [System.Drawing.PointF]::new($_.X * $k, $_.Y * $k)
    })
    $isaretYolu.AddLines($noktalar)

    $kalem = New-Object System.Drawing.Pen($ISARET_RENK, ($ISARET_KALINLIK * $k))
    $kalem.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $kalem.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $kalem.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    $g.DrawPath($kalem, $isaretYolu)

    $kalem.Dispose()
    $fircasi.Dispose()
    $kartYolu.Dispose()
    $isaretYolu.Dispose()
    $g.Dispose()
    return $bmp
}

function Kucult {
    param($kaynak, [int]$boyut)
    $hedef = New-Object System.Drawing.Bitmap($boyut, $boyut)
    $g = [System.Drawing.Graphics]::FromImage($hedef)
    $g.SmoothingMode = 'AntiAlias'
    $g.InterpolationMode = 'HighQualityBicubic'
    $g.PixelOffsetMode = 'HighQuality'
    $g.DrawImage($kaynak, 0, 0, $boyut, $boyut)
    $g.Dispose()
    return $hedef
}

$kareUst = Ust-Ciz $false
$yUst = Ust-Ciz $true

foreach ($dpi in $BOYUTLAR.Keys | Sort-Object) {
    $klasor = Join-Path $res ("mipmap-$dpi")
    $boyut = $BOYUTLAR[$dpi]

    # Kare surum
    $kare = Kucult $kareUst $boyut
    $kare.Save((Join-Path $klasor 'ic_launcher.png'),
        [System.Drawing.Imaging.ImageFormat]::Png)
    $kare.Dispose()

    # Yuvarlak surum
    $yuvarlak = Kucult $yUst $boyut
    $yuvarlak.Save((Join-Path $klasor 'ic_launcher_round.png'),
        [System.Drawing.Imaging.ImageFormat]::Png)
    $yuvarlak.Dispose()

    # Sablon webp'leri artik kullanilmiyor
    foreach ($ad in @('ic_launcher.webp', 'ic_launcher_round.webp')) {
        $eski = Join-Path $klasor $ad
        if (Test-Path $eski) { Remove-Item $eski -Force }
    }
    Write-Host "  $dpi -> $boyut px"
}

$kareUst.Dispose()
$yUst.Dispose()
Write-Host "Tamam: $res\mipmap-*\ic_launcher{,_round}.png"
