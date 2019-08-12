

call ..\setEnv.bat
call .\jvm.bat
if "%JAVA%" == "" goto usage
call .\compile.bat

Set APP_PROPS=-Djetty.home=%JETTY_HOME% -Dcom.kodewerk.stocks.properties=stocksdb.properties

%JAVA% -version
%JAVA% %FLAGS% %APP_PROPS% -jar %JETTY_HOME%\start.jar .\jetty\etc\jetty.xml

goto end

:usage
echo "Java executable must be specified in setEnv.bat."

:end
pause
