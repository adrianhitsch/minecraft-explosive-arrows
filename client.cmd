@echo off
rem Starts Minecraft with the mod (development client, world is stored under run\)
cd /d "%~dp0"
call "%~dp0scripts\find-java.cmd" || (pause & exit /b 1)
call "%~dp0gradlew.bat" runClient
if errorlevel 1 pause
