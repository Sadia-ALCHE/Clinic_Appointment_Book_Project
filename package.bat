:: package.bat - Automated Self-Contained Desktop Packaging Script
:: Builds fat JAR and packages native bundle with embedded runtime for zero-Java machines
@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   MediCare Clinic - Packaging Self-Contained Bundle
echo ========================================================

:: Ensure Java and Maven tools are available in PATH
if not defined JAVA_HOME (
    if exist "C:\Program Files\Java\jdk-26.0.1" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.1"
    )
)
if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)
if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin" (
    set "PATH=C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin;%PATH%"
)

echo 1. Cleaning and compiling package with Maven...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven build failed!
    exit /b %ERRORLEVEL%
)

echo 2. Packaging native desktop bundle with jpackage...
if exist target\dist rmdir /s /q target\dist
mkdir target\dist

jpackage ^
  --type app-image ^
  --name "MediCareClinic" ^
  --input target ^
  --main-jar clinic-appointment-book-1.0.0.jar ^
  --main-class com.clinic.Main ^
  --dest target\dist ^
  --vendor "ALCHE Mauritius - Hanif and Sadia" ^
  --description "Campus Health and Consultation Suite"

if %ERRORLEVEL% EQU 0 (
    echo ========================================================
    echo   BUILD SUCCESS! Self-contained application ready at:
    echo   target\dist\MediCareClinic\MediCareClinic.exe
    echo ========================================================
) else (
    echo [ERROR] jpackage failed!
)
