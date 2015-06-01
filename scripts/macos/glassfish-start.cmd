#!/bin/bash

# The current directory where this command has been executed from
current_dir=`dirname $0`
#pushd "$dir" > /dev/null
cd ./.

base_dir=$(pwd)

cd $base_dir"/glassfish/bin"

"./asadmin" start-domain --verbose domain1