#!/bin/sh

. ../setEnv.sh

export HSQLDB_HOME=.
$JAVA -jar $HSQLDB_HOME/lib/hsqldb.jar mem
