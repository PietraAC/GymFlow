@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
title GymFlow - Parar ambiente local

set "KEEP_DOCKER=0"
set "QUIET=0"
if /I "%~1"=="--keep-docker" set "KEEP_DOCKER=1"
if /I "%~2"=="--keep-docker" set "KEEP_DOCKER=1"
if /I "%~1"=="--quiet" set "QUIET=1"
if /I "%~2"=="--quiet" set "QUIET=1"
set "RUN_DIR=%CD%\.run"

if "%QUIET%"=="0" echo Encerrando processos locais do GymFlow...
call :stop_owned "frontend" "npm.cmd"
call :stop_owned "assistant-service" "assistant-service-0.1.0.jar"
call :stop_owned "workout-service" "workout-service-0.1.0.jar"
call :stop_owned "gym-service" "gym-service-0.1.0.jar"
rem Recupera processos de uma execucao anterior mesmo se o PID file tiver sido perdido.
powershell -NoProfile -ExecutionPolicy Bypass -Command "$root='%CD%'; $owned=Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object { $_.ProcessId -ne $PID -and $_.CommandLine -and $_.CommandLine.Contains($root) -and ($_.CommandLine -like '*gym-service-0.1.0.jar*' -or $_.CommandLine -like '*workout-service-0.1.0.jar*' -or $_.CommandLine -like '*assistant-service-0.1.0.jar*' -or $_.CommandLine -like '*@angular*cli*ng.js*serve*proxy-config*') }; foreach($p in $owned){ & taskkill.exe /PID $p.ProcessId /T /F | Out-Null }"

if "%KEEP_DOCKER%"=="0" (
  if "%QUIET%"=="0" echo Parando containers do GymFlow sem apagar os dados...
  docker info >nul 2>&1
  if not errorlevel 1 docker compose down
)

if "%QUIET%"=="0" (
  echo.
  echo GymFlow foi encerrado. O volume PostgreSQL foi preservado.
)
exit /b 0

:stop_owned
set "PROCESS_NAME=%~1"
set "PROCESS_MARKER=%~2"
set "PID_FILE=%RUN_DIR%\%PROCESS_NAME%.pid"
if not exist "%PID_FILE%" exit /b 0
set "OWNED_PID="
set /p OWNED_PID=<"%PID_FILE%"
if defined OWNED_PID (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$p=Get-CimInstance Win32_Process -Filter ('ProcessId=' + $env:OWNED_PID) -ErrorAction SilentlyContinue; if($p -and ($p.CommandLine -like ('*' + $env:PROCESS_MARKER + '*'))){ & taskkill.exe /PID $p.ProcessId /T /F | Out-Null }"
)
del /Q "%PID_FILE%" >nul 2>&1
exit /b 0
