#!/bin/sh

. ../setEnv.sh
export CP="-cp lib/hsqldb.jar:./classes"
export PROPS="-Dcom.kodewerk.tipsdb.properties=tipsdb.properties"
export MAIN="com.kodewerk.tipsdb.DBMain"
$JAVA $CP $PROPS $MAIN STARTUP
