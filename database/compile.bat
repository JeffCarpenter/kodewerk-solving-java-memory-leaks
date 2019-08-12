
call ..\setEnv.bat
if "%JAVA%" == "" goto usage

set UTIL=src\com\kodewerk\util\*.java
set TIPSDB=src\com\kodewerk\tipsdb\*.java
set DOMAIN=src\com\kodewerk\tipsdb\domain\*.java
set QUERY=src\com\kodewerk\tipsdb\query\*.java

set SOURCES=%UTIL% %TIPSDB% %DOMAIN% %QUERY%

set EXPLODED=.\classes


set CP=-classpath lib\hsqldb.jar;

%JAVAC% %CP% -d %EXPLODED% %SOURCES% 
goto end

:usage
echo Java executable must be specified in setEnv.bat

:end
pause
