#!/bin/sh

set -e

CLASSPATH="/home/local/Documents/machine_learning/weka-3-8-0/weka.jar"

OPTIND=2
while getopts ":t:" opt; do
    case $opt in
        t) filename=$(basename "$OPTARG")
           ext="${filename##*.}"
           filename="${filename%.*}"
           TFILE=$filename".threshold."$ext
           ;;
    esac
done
java -cp $CLASSPATH "$@" -threshold-file $TFILE -threshold-label "85"





