@echo off
rem ---------------------------------------------------------------------------
rem  Droid Rampage - Starter for Windows
rem
rem  1. Uses Java 17 or newer if it is already installed.
rem  2. Otherwise downloads a private Java runtime (Eclipse Temurin, ~45 MB) into
rem     the "java-runtime" folder next to this file. Nothing gets installed on
rem     the system; delete the folder to remove it again.
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"
title Droid Rampage

set "JAR=DroidRampage.jar"
set "RUNTIME_DIR=java-runtime"
set "JAVA_FEATURE=21"
set "JAVAW="

if not exist "%JAR%" (
    echo ERROR: %JAR% not found. Keep this file in the same folder as %JAR%.
    goto :fail
)

rem 1) Previously downloaded runtime
call :find_local_runtime
if defined JAVAW goto :run

rem 2) Installed Java (on the PATH)
where java >nul 2>nul
if not errorlevel 1 (
    call :check_system_java
)
if defined JAVAW goto :run

rem 3) Download a private runtime
echo Java 17+ was not found.
echo Downloading a private Java runtime (about 45 MB, only needed once) ...
set "URL=https://api.adoptium.net/v3/binary/latest/%JAVA_FEATURE%/ga/windows/x64/jre/hotspot/normal/eclipse"
if exist "%RUNTIME_DIR%" rmdir /s /q "%RUNTIME_DIR%"
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop'; $ProgressPreference = 'SilentlyContinue';" ^
  "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12;" ^
  "Invoke-WebRequest -UseBasicParsing -Uri '%URL%' -OutFile 'java-runtime-download.zip';" ^
  "Expand-Archive -Force 'java-runtime-download.zip' '%RUNTIME_DIR%';" ^
  "Remove-Item 'java-runtime-download.zip'"
if errorlevel 1 (
    if exist "java-runtime-download.zip" del "java-runtime-download.zip"
    echo ERROR: Download failed. Check your internet connection.
    goto :fail
)
call :find_local_runtime
if not defined JAVAW (
    echo ERROR: The downloaded Java runtime could not be found.
    goto :fail
)
echo Java is ready.

:run
echo Starting Droid Rampage ...
start "" "%JAVAW%" -jar "%JAR%"
exit /b 0

:fail
echo.
echo You can also install Java 17+ yourself (https://adoptium.net) and double-click %JAR%.
pause
exit /b 1

rem ---------------------------------------------------------------------------

:find_local_runtime
if not exist "%RUNTIME_DIR%" exit /b 0
for /r "%RUNTIME_DIR%" %%f in (javaw.exe) do (
    if exist "%%f" if not defined JAVAW set "JAVAW=%%f"
)
exit /b 0

:check_system_java
rem Reads the version from e.g.:  openjdk version "21.0.2" 2024-01-16   or   java version "1.8.0_401"
set "VERSION="
set "MAJOR="
set "MAJOR_NUM=0"
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    if not defined VERSION set "VERSION=%%~v"
)
if not defined VERSION exit /b 0
for /f "delims=.-_+" %%m in ("%VERSION%") do set "MAJOR=%%m"
if "%MAJOR%"=="1" exit /b 0
set /a MAJOR_NUM=%MAJOR% 2>nul
if %MAJOR_NUM% GEQ 17 (
    for /f "delims=" %%p in ('where javaw 2^>nul') do (
        if not defined JAVAW set "JAVAW=%%p"
    )
)
exit /b 0
