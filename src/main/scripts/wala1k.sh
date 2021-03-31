#!/bin/bash

DIR=`dirname $0`

lst=$1
dir=$2

shift; shift

hosts="$@"
nodes=${#hosts}
slices=`expr 50 '*' $nodes`

slice=0
for host in $host; do
    ssh $host $DIR/wala_node.sh $lst $dir $slice $slices &
    slice=`expr $slice + 50`
done

wait

for f in `ls $dir/11/* | fgrep -v .csv`; do
    d=`basename $f`
    gawk -f src/main/scripts/to_numbers.awk $dir/*/$d
done

for f in $dir/11/*.csv; do
    stem=`basename $f`
    cat $dir/*/$stem > $dir/$stem
done

gzip $dir/*.csv
