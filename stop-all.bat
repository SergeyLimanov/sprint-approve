@echo off
echo Stopping Sprint Approve Microservices...
echo.

echo Stopping Docker containers...
docker-compose down

echo.
echo Please close all Spring Boot application windows manually.
echo.
pause
