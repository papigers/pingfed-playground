@echo off
rem Copyright (C) 2006-2008 Ping Identity Corporation
rem All rights reserved.

if not "%JAVA_HOME%" == "" goto TEST_VERSION
echo JAVA_HOME is not set.  Unexpected results may occur.
echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
pause
goto :eof

:TEST_VERSION

set MINIMUM_JAVA_VERSION=1.8

set JAVA_VERSION=
"%JAVA_HOME%/bin/java" -version 2>java_version.txt
for /f "tokens=3" %%g in (java_version.txt) do (
    del java_version.txt
    set JAVA_VERSION=%%g
    goto CHECK_JAVA_VERSION
)

rem grab first 3 characters of version number (ex: 1.6) and compare against required version
:CHECK_JAVA_VERSION
set JAVA_VERSION=%JAVA_VERSION:~1,3%
if %JAVA_VERSION% == 1.8 (
    set JAVA=%JAVA_HOME%/bin/java
    goto install
    )

:WRONG_JAVA_VERSION
echo JDK %MINIMUM_JAVA_VERSION% or higher is required to run PingFederate but %JAVA_VERSION% was detected. Please set the JAVA_HOME environment variable to a JDK %MINIMUM_JAVA_VERSION% or higher installation directory path.
exit /B 1

:install
if "%MEMTOT%"=="" call ..\..\bin\getMemAndCPUInfo.bat
"%JAVA%" -classpath ..\wrapper\windows-service-configurator.jar com.pingidentity.windows.install.WindowsServiceConfigurator %TOTCORES% %MEMTOT%
copy ..\wrapper\InstallPingFederateService.bat InstallPingFederateServiceTmp.bat > out.txt
call InstallPingFederateServiceTmp.bat
del  InstallPingFederateServiceTmp.bat
del  out.txt
