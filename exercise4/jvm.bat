REM ##################################################

REM File is set into 4 sections
REM 1: collector choices
REM 2: memory pool configurations
REM 3: log settings
REM 4: JITWatch settings

REM ##################################################
REM Collectors

REM Select one collector for YOUNG and one for TENURED
REM or, set to G1
REM
REM SET YOUNG=-XX:+UseParNewGC
REM SET TENURED=-XX:+UseConcMarkSweepGC

REM SET COLLECTORS=%YOUNG% %TENURED%

REM SET COLLECTORS=-XX:+UseG1GC


REM ##################################################
REM Memory Pool configurations

REM configures the maximum size of the Java heap via the -mx flag
REM The current setting is 1 Gigabyte
REM SET MAX_HEAP_SIZE=-mx1g

REM configures the ratio of tenured to young generation by setting the -XX:NewRatio flag.
REM The default setting is 2
REM SET NEW_RATIO=-XX:NewRatio=1

REM configures the size of the young generation by setting the -XX:NewSize flag.
REM SET NEW_SIZE=-Xmn512m

REM configures the size of the survivor spaces as a ratio. The value of N spilt the space into 
REM N + 2 chunks of which N is given to eden and 1 is given to S0 and 1 to S1.
REM configures -XX:SurvivorRatio. The default value is 8
REM SET SURVIVOR_RATIO=-XX:SurvivorRatio=1

REM configure the max tenuring threshold
REM SET MAX_TENURING=-XX:MaxTenuringThreshold=15

REM set any other JVM flags that may need to be configured here
REM SET OTHER_MEMORY_FLAGS=""

SET MEMORY=%MAX_HEAP_SIZE% %NEW_RATIO% %NEW_SIZE% %SURVIVOR_RATIO% %MAX_TENURING% %OTHER_MEMORY_FLAGS%

REM ##################################################
REM GC logging options
REM SET LOG_FILE_NAME=-Xloggc:gc.log
REM SET LOG_SETTINGS="-XX:+PrintGCDetails -XX:+PrintTenuringDistribution"

SET GC_LOGGING=%LOG_FILE_NAME% %LOG_SETTINGS%


REM ##################################################
REM JITWatch Configurations

SET JITWATCH=-XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation -XX:+TraceClassLoading -XX:+PrintAssembly

##################################################
# JFR Configurations
REM SET UNLOCK_JFR=-XX:+UnlockCommercialFeatures -XX:+FlightRecorder
REM SET START_JFR=-XX:StartFlightRecording=duration=60s,filename=exercise1.jfr

REM SET JFR=%$UNLOCK_JFR% %START_JFR%
