@echo off
rem Checks that Gradle can find a JDK: JAVA_HOME (if valid), otherwise java on the PATH.
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" exit /b 0
where java >nul 2>nul && exit /b 0
echo No Java found. Please install JDK 25, e.g. from https://adoptium.net
echo and then set JAVA_HOME or add java to the PATH.
exit /b 1
