#!/bin/sh

##### Edit this path
##### The path should point to the root directory of your Java 8 installation
##### 
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/


#### DO NOT EDIT BELOW THIS COMMENT
#### Values will be derived from the JAVA_HOME setting from above
export JAVA=$JAVA_HOME/bin/java
export JAVAC=$JAVA_HOME/bin/javac
export JAVA_SRC=$JAVA_HOME/src.jar

export JETTY_HOME=../bin/jetty
export JETTY_JARS=$JETTY_HOME/lib/jetty-6.1.26RC0.jar:$JETTY_HOME/lib/jetty-util-6.1.26RC0.jar:$JETTY_HOME/lib/servlet-api-2.5-20081211.jar

$JAVA -version

