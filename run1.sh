#!/bin/sh
CLASSPATH=.:weka.jar:sqlite-jdbc-3.15.1.jar:RCaller-3.1-SNAPSHOT-jar-with-dependencies.jar
javac -cp $CLASSPATH EqualError.java
java -cp $CLASSPATH EqualError
