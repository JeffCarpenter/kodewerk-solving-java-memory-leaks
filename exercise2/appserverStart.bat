

call ..\setEnv.bat
call .\jvm.bat
if "%JAVA%" == "" goto usage
call ./compile.bat

SET APP_PROPS=-Dcom.kodewerk.tipsdb.properties=tipsdb.properties -Djetty.home=%JETTY_HOME%

%JAVA% -version
%JAVA% %FLAGS% %APP_PROPS% -jar %JETTY_HOME%\start.jar .\jetty\etc\jetty.xml

goto end

:usage
echo "Java executable must be specified in setEnv.bat."

:end
pause
