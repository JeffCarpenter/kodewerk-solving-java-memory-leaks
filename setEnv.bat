REM ******************************************
REM Set JAVA_HOME to the location of your JDK
REM DOS batches need to have spaces escaped
SET JAVA_HOME=C:\Progra~1\Java\jdk1.8.0_191

REM *******************************************
REM Do not edit below this line....
REM *******************************************

REM Settings for java and javac derived from JAVA_HOME
SET JAVA="%JAVA_HOME%\bin\java"
SET JAVAC="%JAVA_HOME%\bin\javac"

set JETTY_HOME=..\bin\jetty
set JETTY_JARS=%JETTY_HOME%\lib\jetty-6.1.26RC0.jar;%JETTY_HOME%\lib\jetty-util-6.1.26RC0.jar;%JETTY_HOME%\lib\servlet-api-2.5-20081211.jar

%JAVA% -version
