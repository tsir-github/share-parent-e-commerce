@echo off
chcp 65001 >nul
title share-parent Middleware - Stop
setlocal enabledelayedexpansion

echo Stopping Middleware Services...
echo.

set "PIDFILE=%TEMP%\share-middleware-pids.txt"

:: 1. 通过 PID 文件清理
if exist "%PIDFILE%" (
    echo [PID file found - killing tracked processes...]
    for /f %%i in (%PIDFILE%) do (
        taskkill /F /PID %%i >nul 2>&1
        if !ERRORLEVEL! EQU 0 (
            echo   Killed PID %%i
        )
    )
    del "%PIDFILE%"
) else (
    echo [No PID file - scanning for orphaned processes...]
)

:: 2. 扫一遍 seata/sentinel Java 进程
echo.
echo [Scanning for remaining middleware processes...]
taskkill /F /FI "WINDOWTITLE eq sentinel*" >nul 2>&1
taskkill /F /FI "WINDOWTITLE eq seata*" >nul 2>&1

:: 3. 更精确的 fallback：扫描命令行参数
for /f "tokens=2 delims== " %%a in (
    'wmic process where "name='java.exe' and (CommandLine like '%%sentinel-dashboard%%' or CommandLine like '%%seata-server%%')" get ProcessId /value 2^>nul ^| findstr "ProcessId"'
) do (
    taskkill /F /PID %%a >nul 2>&1
    if !ERRORLEVEL! EQU 0 echo   Killed Java PID %%a
)

echo.
echo Done.
timeout /t 2 /nobreak >nul
