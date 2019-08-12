#!/bin/sh


. ../setEnv.sh
. ./jvm.conf
. ./compile.sh

#export GCLOG="-Xloggc:gc.log -XX:+PrintGCDetails -XX:+PrintTenuringDistribution"
#export MEMORY=""


export FLAGS="$COLLECTORS $MEMORY $GC_LOGGING"
export APP_PROPS="-Djetty.home=$JETTY_HOME -Dcom.kodewerk.stocks.properties=stocksdb.properties"

echo "java $FLAGS $APP_PROPS -jar $JETTY_HOME/start.jar ./jetty/etc/jetty.xml"
$JAVA $FLAGS $APP_PROPS -jar $JETTY_HOME/start.jar ./jetty/etc/jetty.xml
