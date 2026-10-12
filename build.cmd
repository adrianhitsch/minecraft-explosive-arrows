@echo off
rem Builds the mod and runs the GameTests. Output: build\libs\explosivearrows-<version>.jar
rem Paths are relative to this file, so it works from any location.
cd /d "%~dp0"
call "%~dp0scripts\find-java.cmd" || (pause & exit /b 1)
call "%~dp0gradlew.bat" build
if errorlevel 1 (
	echo.
	echo BUILD FAILED
) else (
	echo.
	echo Done. Mod file:
	dir /b build\libs\*.jar | findstr /v sources
	explorer build\libs
)
pause
