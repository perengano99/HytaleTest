@echo off
setlocal

REM --- Configuración Batch ---
set "BASE_DIR=C:\Users\peren\Desktop\Zones"
set "OUTPUT_FILE=%BASE_DIR%\ListaNombres.txt"

REM --- Lanzador Híbrido ---
REM Lee este mismo archivo (%~f0), salta las primeras 12 líneas (el header Batch) y ejecuta el resto.
powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "$code = Get-Content '%~f0' | Select-Object -Skip 12; Invoke-Expression ($code -join [Environment]::NewLine)"
echo.
pause
goto :EOF

# --- ZONA POWERSHELL (Línea 13 en adelante) ---

# Recuperamos las variables de entorno definidas arriba
$baseDir = $env:BASE_DIR
$outFile = $env:OUTPUT_FILE

Write-Host "Jale iniciado en: $baseDir" -ForegroundColor Cyan

if (-not (Test-Path $baseDir)) {
    Write-Host "Error: No encuentro la carpeta base, checa la ruta." -ForegroundColor Red
    exit
}

if (Test-Path $outFile) { Remove-Item $outFile }

# Obtenemos carpetas, asegurando que sea array @() para evitar broncas si solo hay una
$folders = @(Get-ChildItem -Path $baseDir -Recurse -Directory)
# Agregamos la raíz manualmente a la lista
$allPaths = @($baseDir) + $folders.FullName

foreach ($folder in $allPaths) {
    if (-not $folder) { continue }
    
    $folderName = Split-Path $folder -Leaf
    
    # Tu filtro de archivos
    $files = Get-ChildItem -Path $folder -Filter *.json | Where-Object { $_.Name -like 'Tile.*.json' -or $_.Name -like 'Custom.*.json' }
    
    if ($files.Count -gt 0) {
        Add-Content -Path $outFile -Value ("// " + $folderName)
        $ordered = $files | Sort-Object Name
        
        foreach ($file in $ordered) {
            # Lógica corregida para el split
            try {
                $name = $file.Name.Split('.', 2)[1].Replace('.json', '')
                Add-Content -Path $outFile -Value $name
            } catch {
                Write-Host "Error procesando $($file.Name)" -ForegroundColor Yellow
            }
        }
        Add-Content -Path $outFile -Value ""
    }
}

Write-Host "Fierro pariente. Archivo generado en: $outFile" -ForegroundColor Green
Start-Process notepad $outFile