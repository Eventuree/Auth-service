@echo off
setlocal enabledelayedexpansion

:: Default network name or take from first argument
set NETWORK_NAME=%1
if "%1"=="" set NETWORK_NAME=mp-crm-network

:: Check if network exists
docker network inspect %NETWORK_NAME% >nul 2>&1
if errorlevel 1 (
    echo Error: Network %NETWORK_NAME% not found!
    exit /b 1
)

:: Get all container IDs in the network and stop them
echo Finding containers in network %NETWORK_NAME%...
set "CONTAINERS_FOUND="
for /f "tokens=*" %%i in ('docker network inspect -f "{{range .Containers}}{{.Name}} {{end}}" %NETWORK_NAME%') do (
    set "CONTAINERS_FOUND=yes"
    for %%a in (%%i) do (
        echo Stopping container: %%a
        docker stop %%a >nul 2>&1
        if errorlevel 1 (
            echo Warning: Failed to stop container %%a
        ) else (
            echo Successfully stopped container: %%a
        )
    )
)

if not defined CONTAINERS_FOUND (
    echo No containers found in network %NETWORK_NAME%
    exit /b 0
)

echo.
echo All containers in network %NETWORK_NAME% have been stopped.
echo.

:: Optional: Remove the containers as well
set /p REMOVE_CONTAINERS="Do you want to remove the stopped containers? (y/n): "
if /i "%REMOVE_CONTAINERS%"=="y" (
    for /f "tokens=*" %%i in ('docker network inspect -f "{{range .Containers}}{{.Name}} {{end}}" %NETWORK_NAME%') do (
        for %%a in (%%i) do (
            echo Removing container: %%a
            docker rm %%a >nul 2>&1
            if errorlevel 1 (
                echo Warning: Failed to remove container %%a
            ) else (
                echo Successfully removed container: %%a
            )
        )
    )
    echo All containers have been removed.
)

echo.
echo Operation completed.