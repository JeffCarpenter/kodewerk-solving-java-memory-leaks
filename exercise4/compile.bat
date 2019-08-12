
call ..\setEnv.bat
if "%JAVA%" == "" goto usage

set UTIL=src\com\kodewerk\util\*.java
set TIPSDB=src\com\kodewerk\tipsdb\*.java
set DOMAIN=src\com\kodewerk\tipsdb\domain\*.java
set QUERY=src\com\kodewerk\tipsdb\query\*.java
set SERVLET=src\com\kodewerk\tipsdb\servlet\*.java
set SOURCES=%UTIL% %TIPSDB% %DOMAIN% %QUERY% %SERVLET%
set EXPLODED=www\tips

set CP=-classpath %JETTY_JARS%;lib\hsqldb.jar

%JAVAC% %CP% -d %EXPLODED%\WEB-INF\classes %SOURCES% 
goto end

:usage
echo Java executable must be specified in setEnv.bat

:end
pause
