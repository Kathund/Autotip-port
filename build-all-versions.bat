@echo off
REM Build Autotip for all supported partner mod versions
REM All builds use the same MC version and Fabric API version from gradle.properties
REM Only the partner mod version changes

echo Building Autotip for all supported partner mod versions...
echo All builds use MC version from gradle.properties, only partner mod changes.
echo.

set PARTNER_VERSIONS=1.21.8 1.21.9 1.21.10 1.21.11

for %%v in (%PARTNER_VERSIONS%) do (
    echo.
    echo ========================================
    echo Building with partner mod for MC %%v
    echo ========================================
    call gradlew.bat remapJar "-PpartnerVersion=%%v"
    if errorlevel 1 (
        echo.
        echo ERROR: Failed to build partner mod version %%v
        pause
        exit /b 1
    )
)

echo.
echo ========================================
echo All builds complete!
echo ========================================
echo.
echo Built JARs:
dir /b build\libs\Autotip-*.jar
echo.
pause
