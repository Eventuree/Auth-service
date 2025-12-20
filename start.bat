@echo off
echo Starting build process...

echo.
echo === Running Maven build ===
call mvnw clean package -DskipTests
if %ERRORLEVEL% NEQ 0 (
    echo Maven build failed!
    pause
    exit /b %ERRORLEVEL%
)
echo Maven build completed successfully!

echo.
echo === Starting Docker Compose ===
docker-compose up -d --build
if %ERRORLEVEL% NEQ 0 (
    echo Docker Compose failed!
    pause
    exit /b %ERRORLEVEL%
)
echo Docker Compose started successfully!

echo.
echo All operations completed successfully!
echo You can now access the application at http://localhost:8080

pause