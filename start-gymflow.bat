@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title GymFlow - Iniciar ambiente local

set "NO_OPEN=0"
if /I "%~1"=="--no-open" set "NO_OPEN=1"
set "RUN_DIR=%CD%\.run"
if not exist "%RUN_DIR%" mkdir "%RUN_DIR%"

echo [1/8] Verificando configuracao local...
if not exist ".env" (
  copy /Y ".env.example" ".env" >nul
  echo Foi criado .env a partir de .env.example.
)
call :load_env
if not defined GYM_DB_PASSWORD goto :env_error
if not defined WORKOUT_DB_PASSWORD goto :env_error
if not defined ASSISTANT_DB_PASSWORD goto :env_error

echo [2/8] Verificando Docker Desktop...
call :ensure_docker
if errorlevel 1 goto :docker_error

echo [3/8] Encerrando processos antigos do GymFlow...
call "%~dp0stop-gymflow.bat" --keep-docker --quiet

echo [4/8] Iniciando PostgreSQL, Keycloak e RabbitMQ...
docker compose up -d
if errorlevel 1 goto :compose_error
call :wait_url "Keycloak" "http://localhost:8080/realms/gymflow/.well-known/openid-configuration" 90
if errorlevel 1 goto :keycloak_error
call :wait_url "RabbitMQ" "http://localhost:15672" 60
if errorlevel 1 goto :rabbitmq_error

echo [5/8] Localizando Java 21 e compilando os servicos...
call :find_java
if errorlevel 1 goto :java_error
set "JAVA_HOME=%JAVA_HOME_FOUND%"
set "MAVEN_USER_HOME=%CD%\.m2"
call "%CD%\mvnw.cmd" "-Dmaven.repo.local=%CD%\.m2\repository" -DskipTests package
if errorlevel 1 goto :build_error

echo [6/8] Verificando dependencias do frontend...
where npm.cmd >nul 2>&1
if errorlevel 1 goto :node_error
if not exist "frontend\node_modules\.bin\ng.cmd" (
  pushd frontend
  call npm.cmd ci --cache "..\.npm-cache"
  set "NPM_RESULT=!ERRORLEVEL!"
  popd
  if not "!NPM_RESULT!"=="0" goto :npm_error
)

echo [7/8] Iniciando APIs e frontend...
del /Q "%RUN_DIR%\*.pid" "%RUN_DIR%\*.log" 2>nul
set "GYM_SERVICE_URL=http://localhost:8081"
set "WORKOUT_SERVICE_URL=http://localhost:8082"
set "ASSISTANT_SERVICE_URL=http://localhost:8083"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $root='%CD%'; $run='%RUN_DIR%'; $services=@(@{Name='gym-service';Jar='services\gym-service\target\gym-service-0.1.0.jar'},@{Name='workout-service';Jar='services\workout-service\target\workout-service-0.1.0.jar'},@{Name='assistant-service';Jar='services\assistant-service\target\assistant-service-0.1.0.jar'}); foreach($service in $services){ $p=Start-Process -FilePath $env:JAVA_EXE -ArgumentList @('-jar',(Join-Path $root $service.Jar)) -WorkingDirectory $root -RedirectStandardOutput (Join-Path $run ($service.Name+'.log')) -RedirectStandardError (Join-Path $run ($service.Name+'.err.log')) -WindowStyle Hidden -PassThru; Set-Content -LiteralPath (Join-Path $run ($service.Name+'.pid')) -Value $p.Id -Encoding ascii }; $p=Start-Process -FilePath (Get-Command npm.cmd).Source -ArgumentList @('start') -WorkingDirectory (Join-Path $root 'frontend') -RedirectStandardOutput (Join-Path $run 'frontend.log') -RedirectStandardError (Join-Path $run 'frontend.err.log') -WindowStyle Hidden -PassThru; Set-Content -LiteralPath (Join-Path $run 'frontend.pid') -Value $p.Id -Encoding ascii"
if errorlevel 1 goto :process_error

echo [8/8] Aguardando aplicacao ficar pronta...
call :wait_url "gym-service" "http://localhost:8081/actuator/health" 60
if errorlevel 1 goto :health_error
call :wait_url "workout-service" "http://localhost:8082/actuator/health" 60
if errorlevel 1 goto :health_error
call :wait_url "assistant-service" "http://localhost:8083/actuator/health" 60
if errorlevel 1 goto :health_error
call :wait_url "frontend" "http://localhost:4200" 60
if errorlevel 1 goto :health_error

echo.
echo GymFlow esta pronto:
echo   Frontend:          http://localhost:4200
echo   Keycloak:          http://localhost:8080
echo   RabbitMQ:          http://localhost:15672
echo   Gym API:           http://localhost:8081
echo   Workout API:       http://localhost:8082
echo   Assistant API:     http://localhost:8083/swagger-ui.html
echo   Logs:              %RUN_DIR%
echo.
if "%NO_OPEN%"=="0" start "" "http://localhost:4200"
exit /b 0

:load_env
for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do (
  if not "%%A"=="" set "%%A=%%B"
)
exit /b 0

:ensure_docker
docker info >nul 2>&1
if not errorlevel 1 exit /b 0
set "DOCKER_DESKTOP=%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
if not exist "%DOCKER_DESKTOP%" set "DOCKER_DESKTOP=%LocalAppData%\Docker\Docker Desktop.exe"
if not exist "%DOCKER_DESKTOP%" exit /b 1
echo Docker Desktop nao esta ativo; iniciando...
start "" /min "%DOCKER_DESKTOP%"
for /L %%I in (1,1,90) do (
  docker info >nul 2>&1
  if not errorlevel 1 exit /b 0
  ping.exe 127.0.0.1 -n 3 >nul
)
exit /b 1

:find_java
set "JAVA_HOME_FOUND="
for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-21*") do set "JAVA_HOME_FOUND=%%~fD"
if not defined JAVA_HOME_FOUND for /d %%D in ("%ProgramFiles%\Java\jdk-21*") do set "JAVA_HOME_FOUND=%%~fD"
if not defined JAVA_HOME_FOUND if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_HOME_FOUND=%JAVA_HOME%"
if not defined JAVA_HOME_FOUND exit /b 1
set "JAVA_EXE=%JAVA_HOME_FOUND%\bin\java.exe"
if not exist "%JAVA_EXE%" exit /b 1
exit /b 0

:wait_url
set "WAIT_NAME=%~1"
set "WAIT_URL=%~2"
set "WAIT_TRIES=%~3"
for /L %%I in (1,1,%WAIT_TRIES%) do (
  curl.exe --fail --silent --show-error --max-time 2 "%WAIT_URL%" >nul 2>&1
  if not errorlevel 1 exit /b 0
  ping.exe 127.0.0.1 -n 3 >nul
)
echo %WAIT_NAME% nao respondeu em %WAIT_URL%.
exit /b 1

:env_error
echo ERRO: .env nao contem todas as senhas dos tres servicos.
goto :failed
:docker_error
echo ERRO: Docker Desktop nao ficou disponivel.
goto :failed
:compose_error
echo ERRO: nao foi possivel iniciar o Docker Compose.
goto :failed
:keycloak_error
echo ERRO: Keycloak nao ficou pronto. Execute docker compose logs keycloak.
goto :failed
:rabbitmq_error
echo ERRO: RabbitMQ nao ficou pronto. Execute docker compose logs rabbitmq.
goto :failed
:java_error
echo ERRO: JDK 21 nao foi encontrado.
goto :failed
:build_error
echo ERRO: a compilacao Maven falhou.
goto :failed
:node_error
echo ERRO: Node.js/npm nao foi encontrado.
goto :failed
:npm_error
echo ERRO: npm ci falhou.
goto :failed
:process_error
echo ERRO: nao foi possivel iniciar um processo local.
goto :failed
:health_error
echo ERRO: um componente nao ficou pronto. Consulte %RUN_DIR%\*.log.
call "%~dp0stop-gymflow.bat" --keep-docker --quiet
goto :failed
:failed
echo.
pause
exit /b 1
