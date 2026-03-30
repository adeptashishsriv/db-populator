@echo off
setlocal enabledelayedexpansion
echo ==========================================
echo   DB Explorer Distribution Builder
echo   (Bundled JVM via jlink)
echo   JVM: Eclipse Temurin / OpenJDK
echo        GPL v2 + Classpath Exception
echo        Free to redistribute
echo ==========================================
echo.

REM Read version from pom.xml automatically
for /f "usebackq delims=" %%v in (`powershell -NoProfile -Command "(Select-Xml -Path pom.xml -XPath '//*[local-name()=''project'']/*[local-name()=''version'']').Node.InnerText"`) do set APP_VERSION=%%v
set JAR_NAME=db-explorer-%APP_VERSION%.jar
set DIST_DIR=dist-with-jvm
set JRE_DIR=%DIST_DIR%\jre

where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 ( echo Error: mvn not found. & pause & exit /b 1 )
where jlink >nul 2>nul
if %ERRORLEVEL% NEQ 0 ( echo Error: jlink not found. Ensure JDK 17+ is in PATH. & echo Download: https://adoptium.net/ & pause & exit /b 1 )
where jdeps >nul 2>nul
if %ERRORLEVEL% NEQ 0 ( echo Error: jdeps not found. Ensure JDK 17+ is in PATH. & pause & exit /b 1 )

echo [1/5] Building project...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 ( echo Build failed. & pause & exit /b %ERRORLEVEL% )

set JAR_PATH=target\%JAR_NAME%
if not exist "%JAR_PATH%" ( echo Error: JAR not found at %JAR_PATH% & pause & exit /b 1 )

echo.
echo [2/5] Detecting required JDK modules with jdeps...
jdeps --ignore-missing-deps --print-module-deps --multi-release 17 "%JAR_PATH%" > .modules.tmp 2>nul
set /p MODULES=<.modules.tmp
del .modules.tmp

REM Ensure essential modules are always included
set MODULES=%MODULES%,java.desktop,java.naming,java.security.jgss,jdk.crypto.ec
echo Modules: %MODULES%

echo.
echo [3/5] Creating trimmed JRE with jlink...
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%DIST_DIR%"

jlink ^
    --add-modules %MODULES% ^
    --strip-debug ^
    --no-man-pages ^
    --no-header-files ^
    --compress=2 ^
    --output "%JRE_DIR%"
if %ERRORLEVEL% NEQ 0 ( echo jlink failed. & pause & exit /b %ERRORLEVEL% )

echo.
echo [4/5] Copying application files...
copy "%JAR_PATH%"          "%DIST_DIR%\"
if exist USER_HANDBOOK.md      copy USER_HANDBOOK.md      "%DIST_DIR%\"
if exist USER_HANDBOOK.pdf     copy USER_HANDBOOK.pdf     "%DIST_DIR%\"
if exist RELEASE_NOTES.md      copy RELEASE_NOTES.md      "%DIST_DIR%\"
if exist LICENSE_AGREEMENT.txt copy LICENSE_AGREEMENT.txt "%DIST_DIR%\"
copy "%JRE_DIR%\legal\java.base\LICENSE" "%DIST_DIR%\JVM_LICENSE.txt" >nul 2>nul

echo.
echo [5/5] Creating launcher...
(
    echo @echo off
    echo set SCRIPT_DIR=%%~dp0
    echo start "" "%%SCRIPT_DIR%%jre\bin\javaw.exe" -jar "%%SCRIPT_DIR%%%JAR_NAME%%"
) > "%DIST_DIR%\db-explorer.bat"

echo.
echo ==========================================
echo   Done! Output: %DIST_DIR%\
echo ==========================================
echo.
echo Users do NOT need Java installed - JRE is bundled.
echo To distribute: ZIP the entire '%DIST_DIR%' folder.
echo.
pause
