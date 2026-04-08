@echo off
REM Fix .gitignore issue with dist-with-jvm folder
REM This script removes already tracked files from git index

cd /d "C:\Users\ashis\IdeaProjects\db-explorer"

echo.
echo ================================================
echo FIXING .GITIGNORE ISSUE WITH DIST-WITH-JVM
echo ================================================
echo.
echo The dist-with-jvm folder is showing in git changes because
echo the files were already tracked before .gitignore was added.
echo.
echo This script will:
echo 1. Remove the files from git tracking
echo 2. Keep them in your working directory
echo 3. Future builds won't track these files
echo.

REM Check if we're in a git repository
if not exist ".git" (
    echo ERROR: Not a git repository!
    pause
    exit /b 1
)

echo Checking current git status...
git status --porcelain | findstr "dist-with-jvm" >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo No dist-with-jvm files found in git status.
    echo The issue might already be resolved.
    echo.
    goto :check_gitignore
)

echo.
echo Found dist-with-jvm files in git tracking.
echo Removing from git index (keeping files in working directory)...

REM Remove from git index but keep in working directory
git rm --cached -r dist-with-jvm/ 2>nul

if %errorlevel% equ 0 (
    echo.
    echo ✅ SUCCESS: Removed dist-with-jvm from git tracking
    echo.
    echo The files are still in your working directory but
    echo will no longer be tracked by git.
    echo.
) else (
    echo.
    echo ❌ ERROR: Failed to remove from git index
    echo.
    echo You may need to run this manually:
    echo git rm --cached -r dist-with-jvm/
    echo.
)

:check_gitignore
echo Checking .gitignore configuration...
findstr /C:"dist-with-jvm/" .gitignore >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ .gitignore correctly excludes dist-with-jvm/
) else (
    echo ❌ WARNING: dist-with-jvm/ not found in .gitignore
    echo.
    echo Add this line to .gitignore:
    echo dist-with-jvm/
)

echo.
echo Verifying fix...
git status --porcelain | findstr "dist-with-jvm" >nul 2>&1
if %errorlevel% neq 0 (
    echo ✅ SUCCESS: dist-with-jvm no longer tracked by git
) else (
    echo ❌ ISSUE: dist-with-jvm still showing in git status
    echo.
    echo Try running: git rm --cached -r dist-with-jvm/
    echo Then commit the changes.
)

echo.
echo ================================================
echo NEXT STEPS:
echo ================================================
echo.
echo 1. Review the changes: git status
echo 2. Commit the removal: git commit -m "Remove dist-with-jvm from tracking"
echo 3. Future builds won't track these files
echo.
echo The dist-with-jvm folder will remain in your working directory
echo but won't be committed to git anymore.
echo.

pause
