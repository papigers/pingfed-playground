@echo off
REM 
REM This script is used to determine the amount of physical RAM and number of CPU cores available
REM

setlocal enabledelayedexpansion

:freeMem
for /f "skip=1" %%p in ('wmic os get freephysicalmemory') do ( 
  set mem=%%p
  goto :numCPUs
)

:numCPUs
rem Get windows Version numbers
For /f "tokens=2 delims=[]" %%G in ('ver') Do (set version=%%G) 

For /f "tokens=2,3,4 delims=. " %%G in ('echo %version%') Do (set /A major=%%G& set minor=%%H& set build=%%I) 


if %major% EQU 5 goto :win2003
if %major% GEQ 6 goto :win2008

:win2003
set /A CORES=0
for /F "tokens=2 delims==" %%C in ('wmic computersystem get NumberOfProcessors /value ^| findstr NumberOfProcessors') do (set /A NUMCORES=%%C & set /A CORES=CORES+NUMCORES)
goto :end


:win2008
set /A CORES=0
for /F "tokens=2 delims==" %%C in ('wmic cpu get NumberOfCores /value ^| findstr NumberOfCores') do (set /A NUMCORES=%%C & set /A CORES=CORES+NUMCORES)
goto :end

:end

endlocal & set MEMTOT=%MEM% & set TOTCORES=%CORES%
