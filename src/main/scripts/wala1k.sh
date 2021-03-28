#!/bin/bash

lst=$1
dir=$2

for i in 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49; do
    (nohup bash -v src/main/scripts/run.sh $lst $i 50 $dir &);
done

for f in `ls $dir/11/* | fgrep -v .csv`; do
    d=`basename $f`
    gawk -f src/main/scripts/to_numbers.awk $dir/*/$d
done

for f in $dir/11/*.csv; do
    stem=`basename $f`
    cat $dir/*/$stem > $dir/$stem
done

gzip $dir/*.csv
