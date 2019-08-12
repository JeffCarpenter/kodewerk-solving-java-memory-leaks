#!/bin/sh

. ../setEnv.sh

export CLASSPATH=./lib/hsqldb.jar

$JAVA -classpath $CLASSPATH org.hsqldb.Server 
#-database.0 file:db/stocks -dbname.0 stocks
