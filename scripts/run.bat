@echo off
setlocal

set JAR=%~dp0..\out\wifi-chat-client.jar
if not exist "%JAR%" (
  echo Client jar not found. Please run scripts\build.ps1 first.
  exit /b 1
)

java -jar "%JAR%" %*
