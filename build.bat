@echo off
REM Script auxiliar para ejecutar comandos Ant del proyecto Validador ACES
REM Uso: build compile, build jar, build run, etc.

setlocal enabledelayedexpansion

set "ANT_BIN=C:\opt\ant\bin\ant.bat"
set "PROJECT_ROOT=%~dp0"
set "TARGET=%1"

if "%TARGET%"=="" (
    set "TARGET=info"
)

if "%TARGET%"=="help" (
    echo.
    echo ========================================
    echo  Validador ACES - Build Helper
    echo ========================================
    echo.
    echo TARGETS DISPONIBLES:
    echo.
    echo   build compile    - Compilar proyecto
    echo   build clean      - Limpiar archivos compilados
    echo   build jar        - Empaquetar JAR ejecutable
    echo   build run        - Compilar, empaquetar y ejecutar
    echo   build javadoc    - Generar documentacion JavaDoc
    echo   build test       - Ejecutar tests
    echo   build all        - Ejecutar clean, compile, jar, javadoc
    echo   build info       - Mostrar informacion del proyecto
    echo   build help       - Mostrar esta ayuda
    echo.
    echo EJEMPLOS:
    echo.
    echo   build compile    Compilar codigo fuente
    echo   build run        Compilar, empaquetar y ejecutar
    echo   build clean      Limpiar archivos generados
    echo.
    goto :end
)

if not exist "%ANT_BIN%" (
    echo Error: Apache Ant no encontrado en %ANT_BIN%
    echo Por favor instala Apache Ant
    exit /b 1
)

echo Ejecutando target: %TARGET%
echo.

"%ANT_BIN%" %TARGET% -f "%PROJECT_ROOT%build.xml"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Target '%TARGET%' completado exitosamente
) else (
    echo.
    echo Error: Target '%TARGET%' fallo
    exit /b %ERRORLEVEL%
)

:end
