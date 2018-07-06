#!/bin/bash

workingDir="`dirname \"$0\"`"
cd "$workingDir" || exit 1
workingDir=$(pwd)

javac -cp lib/* -d classes src/HelloWorld.java
