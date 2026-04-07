@echo off
echo Starting Sprint Approve Microservices...
echo.

echo Starting databases...
docker-compose up -d
timeout /t 5

echo.
echo Starting Eureka Server...
start "Eureka Server" cmd /k "cd eureka-server && mvn spring-boot:run"
timeout /t 15

echo.
echo Starting Team Service...
start "Team Service" cmd /k "cd team-service && mvn spring-boot:run"
timeout /t 10

echo.
echo Starting Sprint Service...
start "Sprint Service" cmd /k "cd sprint-service && mvn spring-boot:run"
timeout /t 10

echo.
echo Starting Task Service...
start "Task Service" cmd /k "cd task-service && mvn spring-boot:run"
timeout /t 10

echo.
echo Starting API Gateway...
start "API Gateway" cmd /k "cd api-gateway && mvn spring-boot:run"

echo.
echo All services are starting...
echo Eureka Dashboard: http://localhost:8761
echo API Gateway: http://localhost:8080
echo.
pause
