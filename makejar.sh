#!/bin/bash 
cd build/classes
find . -name '*.class' -print > classes.list
jar cmf ../../MANIFEST.MF ../../nmerge.jar @classes.list
cd ../..
