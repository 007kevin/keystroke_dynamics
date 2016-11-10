#!/bin/sh
CLASSPATH=.:../weka.jar:../sqlite-jdbc-3.15.1.jar
javac -cp $CLASSPATH WekaArff.java
java WekaArff
