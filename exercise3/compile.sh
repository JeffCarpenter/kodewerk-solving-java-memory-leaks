#!/bin/sh

. ../setEnv.sh

export DOMAIN="src/com/kodewerk/stock/*.java src/com/kodewerk/web/*.java src/com/kodewerk/db/*.java"
export SOURCES="$DOMAIN"
export EXPLODED="www/lab"


export CP="-classpath $JETTY_JARS:lib/hsqldb.jar"

$JAVAC $CP -d $EXPLODED/WEB-INF/classes $SOURCES 
