#!/bin/sh

. ../setEnv.sh
. ./jvm.conf
. ./compile.sh

export FLAGS="$COLLECTORS $MEMORY $GC_LOGGING"
export APP_PROPS="-Djetty.home=$JETTY_HOME -Dcom.kodewerk.tipsdb.properties=tipsdb.properties"

echo "Settings--->$FLAGS"

echo "java $FLAGS $APP_PROPS -jar $JETTY_HOME/start.jar ./jetty/etc/jetty.xml"
java $FLAGS $APP_PROPS -jar $JETTY_HOME/start.jar ./jetty/etc/jetty.xml

