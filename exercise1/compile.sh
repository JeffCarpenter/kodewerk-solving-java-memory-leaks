#!/bin/sh

. ../setEnv.sh

export UTIL="src/com/kodewerk/util/*.java"
export TIPS="src/com/kodewerk/tipsdb/*.java"
export DOMAIN="src/com/kodewerk/tipsdb/domain/*.java"
export QUERY="src/com/kodewerk/tipsdb/query/*.java"
export SERVLET="src/com/kodewerk/tipsdb/servlet/*.java"
export SOURCES="$UTIL $TIPS $DOMAIN $QUERY $SERVLET"
export EXPLODED="www/tips"

export CP="-classpath $JETTY_JARS:lib/hsqldb.jar"

$JAVAC $CP -d $EXPLODED/WEB-INF/classes $SOURCES 
