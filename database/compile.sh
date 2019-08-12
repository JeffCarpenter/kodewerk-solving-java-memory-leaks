#!/bin/sh

. ../setEnv.sh

export TIPS="src/com/kodewerk/tipsdb/*.java"
export DOMAIN="src/com/kodewerk/tipsdb/domain/*.java"
export QUERY="src/com/kodewerk/tipsdb/query/*.java"
export UTIL="src/com/kodewerk/util/*.java"
export SOURCES="$UTIL $TIPS $DOMAIN $QUERY $SERVLET"
export EXPLODED="./classes"
export CP="-classpath lib/hsqldb.jar"

$JAVAC $CP -d $EXPLODED $SOURCES 
