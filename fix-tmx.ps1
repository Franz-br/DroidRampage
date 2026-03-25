# Fix TMX file for FXGL compatibility after editing in Tiled
# Run this script after saving your map in Tiled!
# Usage: .\fix-tmx.ps1

# Fix both TMX files
$tmxFiles = @(
    "src\main\resources\assets\levels\TileStart.tmx",
    "src\main\resources\assets\levels\Tile1.tmx"
)
$texturesPath = "src\main\resources\assets\textures"

Write-Host "Fixing TMX files for FXGL compatibility..." -ForegroundColor Cyan

foreach ($tmxPath in $tmxFiles) {

$content = Get-Content $tmxPath -Raw

# Find all external tileset references like: <tileset firstgid="X" source="path/to/file.tsx"/>
$regex = '<tileset\s+firstgid="(\d+)"\s+source="([^"]+)"(?:\s*)/>'

$matches_found = [regex]::Matches($content, $regex)

foreach ($match in $matches_found) {
    $firstgid = $match.Groups[1].Value
    $tsxSource = $match.Groups[2].Value
    $originalTag = $match.Value

    # Resolve TSX path relative to the TMX file
    $tmxDir = Split-Path (Resolve-Path $tmxPath) -Parent
    $tsxFullPath = Join-Path $tmxDir $tsxSource

    if (Test-Path $tsxFullPath) {
        Write-Host "  Processing external tileset: $tsxSource (firstgid=$firstgid)" -ForegroundColor Yellow

        # Read the TSX file to get tileset properties
        [xml]$tsx = Get-Content $tsxFullPath
        $ts = $tsx.tileset
        $name = $ts.name
        $tilewidth = $ts.tilewidth
        $tileheight = $ts.tileheight
        $tilecount = $ts.tilecount
        $columns = $ts.columns
        $imgSource = $ts.image.source  # e.g. "sci-fi-tileset.png"
        $imgWidth = $ts.image.width
        $imgHeight = $ts.image.height

        # Get just the filename for FXGL (it looks in assets/textures/)
        $imgFilename = Split-Path $imgSource -Leaf

        # Build embedded tileset XML
        $embedded = " <tileset firstgid=`"$firstgid`" name=`"$name`" tilewidth=`"$tilewidth`" tileheight=`"$tileheight`" tilecount=`"$tilecount`" columns=`"$columns`">`n  <image source=`"$imgFilename`" width=`"$imgWidth`" height=`"$imgHeight`"/>`n </tileset>"

        $content = $content.Replace($originalTag, $embedded)
        Write-Host "    -> Embedded as '$name' with image '$imgFilename'" -ForegroundColor Green
    } else {
        Write-Host "  WARNING: TSX file not found: $tsxFullPath" -ForegroundColor Red
    }
}

    # Also fix any embedded tilesets with tilecount="0" (Tiled bug)
    $content = $content -replace 'tilecount="0"', 'tilecount="4096"'

    # Save the fixed file
    Set-Content -Path $tmxPath -Value $content -NoNewline

    # Also copy to target so you don't need to run mvnw compile
    $targetTmxPath = $tmxPath -replace 'src\\main\\resources', 'target\\classes'
    $targetDir = Split-Path $targetTmxPath -Parent
    if (Test-Path $targetDir) {
        Copy-Item -Path $tmxPath -Destination $targetTmxPath -Force
        Write-Host "  -> Also copied fixed TMX to target/classes." -ForegroundColor Green
    }

    Write-Host "  File fixed successfully!" -ForegroundColor Green
}

Write-Host ""
Write-Host "All TMX files fixed successfully!" -ForegroundColor Green
Write-Host "You can now run the game directly from IntelliJ." -ForegroundColor Cyan
Write-Host "(If you want to use mvnw, set JAVA_HOME first:" -ForegroundColor Gray
Write-Host '  $env:JAVA_HOME = "C:\path\to\your\jdk-25"' -ForegroundColor Gray
Write-Host "  .\mvnw.cmd compile)" -ForegroundColor Gray
