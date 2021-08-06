#!/bin/bash

FILE=`realpath $1`
OUT=`realpath $2`

DATA_DIR=`dirname $FILE`
DATA_NAME=`basename $FILE`

shift; shift

docker run -v $DATA_DIR:/input -v $OUT:/output -ti walacodenet java -cp /home/wala/AnalysisCodeGenerator/target/AnalysisGraphGenerator-0.0.1-SNAPSHOT.jar -DoutputDir=/output "$@" com.ibm.wala.codeNet.WalaToGNNFiles /input/$DATA_NAME
