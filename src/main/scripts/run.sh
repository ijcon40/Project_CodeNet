#!/bin/bash

ME=$0
DIR=`basename $0`
ROOT=`realpath $DIR/../../../`

list=$1
slice=$2
limit=$3
ourdir=$4

JAR=${ROOT}/target/AnalysisGraphGenerator-0.0.1-SNAPSHOT.jar
CLS=com.ibm.wala.codeNet.WalaToGNNFiles

(for f in `awk "NR%${limit}==${slice} { print \\$1; }" ${list}`
 do dir=$ROOT/data/`basename $list`/`basename $f .java`
    jdir=`dirname $f`
    pdir=`dirname $jdir`
    p=`basename $pdir`
    java -cp $JAR -DgraphLabel=$p -DoutputDir=$outdir $CLS -sourceDir=$f -mainClass=LMain
 done
) > /tmp/log.codenet.`basename $list`.${slice}.txt 2>&1
