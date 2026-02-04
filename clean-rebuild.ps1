# Script para limpiar cache corrupto de Gradle y rebuildar

Write-Host "=== Limpiando Gradle corrupto ===" -ForegroundColor Cyan

# 1. Detener daemon
Write-Host "1. Deteniendo Gradle daemon..." -ForegroundColor Yellow
.\gradlew.bat --stop
Start-Sleep -Seconds 2

# 2. Limpiar caches del proyecto
Write-Host "2. Limpiando caches del proyecto..." -ForegroundColor Yellow
Remove-Item -Recurse -Force .gradle -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force app\build -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force build -ErrorAction SilentlyContinue

# 3. Limpiar cache global de Gradle (el corrupto)
Write-Host "3. Limpiando cache global corrupto..." -ForegroundColor Yellow
Remove-Item -Recurse -Force $env:USERPROFILE\.gradle\caches\journal-1 -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force $env:USERPROFILE\.gradle\caches\transforms-3 -ErrorAction SilentlyContinue

# 4. Clean
Write-Host "4. Ejecutando clean..." -ForegroundColor Yellow
.\gradlew.bat clean

# 5. Build
Write-Host "5. Ejecutando build..." -ForegroundColor Green
.\gradlew.bat assembleDebug --stacktrace

Write-Host "`n=== Proceso completado ===" -ForegroundColor Cyan
