#!/bin/bash

DIR=`dirname $0`
DIR=`realpath $DIR`

lst=$1
dir=$2
start_slice=$3
slices=$4

for i in 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49; do
    (nohup bash -v $DIR/run.sh $lst `expr $i + $start_slice` $slices $dir &)
done

wait
