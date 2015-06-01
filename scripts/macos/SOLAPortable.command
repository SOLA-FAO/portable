#!/bin/bash
current_dir=`dirname $0`
pushd "$dir" > /dev/null
cd $current_dir
#echo "current_dir = "$current_dir
./jdk/jre/bin/java -jar ./clients/sola-portable.jar