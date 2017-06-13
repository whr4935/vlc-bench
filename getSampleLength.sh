#!/usr/bin/bash

if [ "$#" -eq 1 ] ; then
    ffprobe -i $1 -show_entries format=duration -v quiet -of csv="p=0"
else
    echo "usage: ./getSampleLength [filepath]"
fi
