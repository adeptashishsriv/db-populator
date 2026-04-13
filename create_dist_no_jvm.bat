@echo off
setlocal
echo ==========================================
echo   DB Explorer Distribution Builder
echo   (No bundled JVM - requires Java 17+)
echo ==========================================
echo.

REM Read version from pom.xml automatically
for /f "usebackq delims=" %%v in (`powershell -NoProfile -Command "(Select-Xml -Path pom.xml -XPath '//*[local-name()=''project'']/*[local-name()=''version'']').Node.InnerText"`) do set APP_VERSION=%%v
set JAR_NAME=db-explorer-%APP_VERSION%.jar
set DIST_DIR=dist-no-jvm

where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Error: Maven not found in PATH.
    pause & exit /b 1
)

echo [1/3] Building project...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo Build failed.
    pause & exit /b %ERRORLEVEL%
)

echo.
echo [2/3] Creating distribution directory...
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%DIST_DIR%"

echo.
echo [3/3] Copying files...
if not exist "target\%JAR_NAME%" (
    echo Error: JAR not found at target\%JAR_NAME%
    pause & exit /b 1
)
copy "target\%JAR_NAME%" "%DIST_DIR%\"
if exist USER_HANDBOOK.md      copy USER_HANDBOOK.md      "%DIST_DIR%\"
if exist USER_HANDBOOK.pdf     copy USER_HANDBOOK.pdf     "%DIST_DIR%\"
if exist RELEASE_NOTES.md      copy RELEASE_NOTES.md      "%DIST_DIR%\"
if exist LICENSE_AGREEMENT.txt copy LICENSE_AGREEMENT.txt "%DIST_DIR%\"

REM Launcher - uses system Java
(
    echo @echo off
    echo java -jar "%JAR_NAME%"
) > "%DIST_DIR%\db-explorer.bat"

echo.
echo ==========================================
echo   Done! Output: %DIST_DIR%\
echo ==========================================
echo.
echo NOTE: Users must have Java 17+ installed to run this package.
echo.
pause
