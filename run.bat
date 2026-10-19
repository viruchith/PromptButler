@echo off
rem Launch the Prompt Butler shadow JAR without keeping a console window open.
rem Usage: run.bat [path\to\prompt-butler-all.jar]
rem Default JAR: build\libs\prompt-butler-*-all.jar

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"

if "%~1"=="" (
    for /f "delims=" %%F in ('dir /b /o-d "%SCRIPT_DIR%build\libs\*-all.jar" 2^>nul') do (
        set "JAR=%SCRIPT_DIR%build\libs\%%F"
        goto :found
    )
    echo ERROR: shadow JAR not found. Run "gradlew.bat shadowJar" first, or pass the path as an argument.
    exit /b 1
) else (
    set "JAR=%~1"
)

:found
if not exist "%JAR%" (
    echo ERROR: JAR not found: %JAR%
    exit /b 1
)

rem Use javaw.exe (no console window) + start "" so this batch window can close.
start "" javaw ^
    --add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED ^
    --add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED ^
    -jar "%JAR%"
