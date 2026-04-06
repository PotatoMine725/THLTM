@echo off
setlocal

set JAR=%~dp0..\out\wifi-chat-server.jar
if not exist "%JAR%" (
  echo Server jar not found. Please run scripts\build.ps1 first.
  exit /b 1
)

java -jar "%JAR%" %*
