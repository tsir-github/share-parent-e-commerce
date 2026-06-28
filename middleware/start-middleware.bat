@echo off
chcp 65001 >nul
title share-parent Middleware
setlocal enabledelayedexpansion

set "BASE=%~dp0"

:: stop ???????
if /i "%1"=="stop" goto :stop

:: ??????
set "PIDFILE=%TEMP%\share-middleware-pids.txt"
if exist "%PIDFILE%" (
    echo [?????????????...]
    call :stop
)

cd /d "%BASE%"

echo ============================================
echo   Starting Middleware Services...
echo ============================================
echo.

echo [1/2] Sentinel Dashboard (8718)...
start "" /B java -Dserver.port=8718 -Dcsp.sentinel.dashboard.server=localhost:8718 -Dproject.name=sentinel-dashboard -jar "%BASE%sentinel-dashboard-1.8.6.jar" > "%BASE%logs\sentinel.log" 2>&1
powershell -NoProfile -Command "(Get-WmiObject Win32_Process -Filter \"CommandLine like '%%sentinel-dashboard%%'\").ProcessId | Out-File -Encoding default '%PIDFILE%'"
echo   OK (PID saved)
echo.

echo [2/2] Seata Server (7091/8091)...
set "JAVA_OPTS=-Xmx512m -Xms512m -Dseata.service.bind-ip=192.168.31.93"
start "" /B "%BASE%seata\bin\seata-server.bat" > "%BASE%logs\seata.log" 2>&1
powershell -NoProfile -Command "(Get-WmiObject Win32_Process -Filter \"Name='java.exe' and CommandLine like '%%seata-server%%'\").ProcessId | Out-File -Encoding default -Append '%PIDFILE%'"
echo   OK (PID saved)
echo.

echo ============================================
echo   All services started in background.
echo.
echo   Sentinel: http://localhost:8718  (sentinel/sentinel)
echo   Seata:    http://localhost:7091  (seata/seata)
echo   Seata TC: 192.168.31.93:8091
echo.
echo   [Press any key] - stop services
echo   [Close window]  - auto cleanup (Win10+)
echo   [Click X fails] - run: start-middleware stop
echo ============================================
echo.

pause >nul

:: ===== ????????? =====
:stop
echo.
echo Stopping services...
if exist "%PIDFILE%" (
    for /f %%i in (%PIDFILE%) do (
        taskkill /F /PID %%i >nul 2>&1
        if !ERRORLEVEL! EQU 0 (
            echo   Killed PID %%i
        ) else (
            echo   PID %%i not found (already stopped)
        )
    )
    del "%PIDFILE%"
)

:: ?????????
taskkill /F /FI "WINDOWTITLE eq sentinel*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq seata*" >nul 2>&1

echo Done.
timeout /t 2 /nobreak >nul
exit /b