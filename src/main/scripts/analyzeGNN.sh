#!/bin/bash

DIR=`dirname $0`

bash $DIR/analyze.sh "$@" -DSDG=true
