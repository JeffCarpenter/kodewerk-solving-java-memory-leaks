
call ..\setEnv.bat
if "%JAVA%" == "" goto usage


set DOMAIN=src/com/kodewerk/stock/*.java src/com/kodewerk/web/*.java src/com/kodewerk/db/*.java
set SOURCES=%DOMAIN%
set EXPLODED=www/lab

set CP=-classpath %JETTY_JARS%;lib\hsqldb.jar

%JAVAC% %CP% -d %EXPLODED%\WEB-INF\classes %SOURCES% 
goto end

:usage
echo Java executable must be specified in setEnv.bat

:end
pause
