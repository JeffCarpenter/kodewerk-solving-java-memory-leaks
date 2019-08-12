
call ..\setEnv.bat
SET CP=-cp lib\hsqldb.jar;classes
SET PROPS=-Dcom.kodewerk.tipsdb.properties=tipsdb.properties
SET MAIN=com.kodewerk.tipsdb.DBMain
%JAVA% %CP% %PROPS% %MAIN% STARTUP

pause