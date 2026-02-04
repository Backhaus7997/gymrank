# GymRank Argentina - Build Script
# Este script configura el entorno y compila el proyecto

Write-Host "🏋️ GymRank Argentina - Build Script" -ForegroundColor Green
Write-Host ""

# Configurar JAVA_HOME usando el JDK de Android Studio
$androidStudioJDK = "C:\Program Files\Android\Android Studio\jbr"

if (Test-Path $androidStudioJDK) {
    Write-Host "✅ Configurando JAVA_HOME..." -ForegroundColor Green
    $env:JAVA_HOME = $androidStudioJDK
    Write-Host "   JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Cyan
} else {
    Write-Host "❌ No se encontró el JDK de Android Studio" -ForegroundColor Red
    Write-Host "   Busca tu instalación de JDK y configura JAVA_HOME manualmente" -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "📦 Verificando Gradle..." -ForegroundColor Green

# Dar permisos de ejecución al wrapper
if (Test-Path ".\gradlew.bat") {
    Write-Host "✅ Gradle Wrapper encontrado" -ForegroundColor Green
} else {
    Write-Host "❌ No se encontró gradlew.bat" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🔨 Compilando proyecto..." -ForegroundColor Green
Write-Host ""

# Ejecutar build
& .\gradlew.bat clean build --warning-mode all

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ ¡Compilación exitosa!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📱 Para instalar en dispositivo/emulador:" -ForegroundColor Cyan
    Write-Host "   .\gradlew.bat installDebug" -ForegroundColor White
    Write-Host ""
    Write-Host "🚀 O abre el proyecto en Android Studio y ejecuta 'Run'" -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "❌ Error en la compilación" -ForegroundColor Red
    Write-Host "   Revisa los errores arriba o abre el proyecto en Android Studio" -ForegroundColor Yellow
}
