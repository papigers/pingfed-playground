@echo off

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT"  setlocal

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0

set PF_HOME=%DIRNAME%..
cd %PF_HOME%

set LIB=lib

set SERVER=server\default
set SERVER_LIB=%SERVER%\lib
set SERVER_DEPLOY=%SERVER%\deploy

set PF_ENDORSED_DIRS=%LIB%\endorsed

set CLASSPATH="%SERVER%\conf";"%SERVER%\data\config-store"

REM Read an optional running configuration file
@if ["%RUN_CONF%"] == [""] (
    set RUN_CONF=%DIRNAME%\conf.bat
)
@if exist "%RUN_CONF%" (
    call "%RUN_CONF%"
)

set CLASSPATH=%CLASSPATH%;%LIB%\*;%SERVER_LIB%\*;%SERVER_DEPLOY%\*

if not exist log mkdir log

if "%JAVA_HOME%" == "" (
	set JAVA=java
	echo JAVA_HOME is not set.  Unexpected results may occur.
	echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) ELSE (
	set JAVA=%JAVA_HOME%\bin\java
)

"%JAVA%" -cp "%CLASSPATH%" -Djava.endorsed.dirs="%PF_ENDORSED_DIRS%" -Dpf.home="%PF_HOME%" -Dpf.server.default.dir="%SERVER%" -Dlog4j.configurationFile="file://%PF_HOME%/bin/provmgr.log4j2.xml" com.pingidentity.provisioner.cli.CommandLineTool %*

goto :eof