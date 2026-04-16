@echo off
setlocal
set DIR=%~dp0
set PROFILE=%COMPUTERNAME%
if not "%~1"=="" set PROFILE=%~1
if not "%~1"=="" shift
"%DIR%runtime\\bin\\java.exe" -jar "%DIR%wifi-chat-client.jar" --mode hybrid --server-host 167.71.201.89 --server-port 61000 --group 239.255.50.10 --port 50000 --room "General" --profile "%PROFILE%" %*
pause
