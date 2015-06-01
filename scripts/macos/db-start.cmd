#!/bin/bash
# The current directory where this command has been executed from
current_dir=`dirname $0`
#pushd "$dir" > /dev/null
cd ./.
base_dir=$(pwd)
echo "base_dir is "$base_dir

postgres_dir="$current_dir/postgresql"

#PGSQL=$postgres_dir/App/PgSQL/bin/pg_ctl.exe
#PGDATA=$postgres_dir/Data/data
#PGLOG=$base_dir/logs/postgresql.log
#PGLOCALEDIR=$postgres_dir/share
#PGDATABASE=postgres
#PGUSER=postgres

echo
echo 'Start Postgres Database using Launch Pad (if it has not started automatically)'
echo 'Check Postgresl Preferences (Elephant icon on the menu bar at top of screen)'
echo 'Data Directory should point to postgresl data sub-folder in SOLAPortable folder'
echo 'and be something like  /Users/neilpullar/Downloads/SOLAPortableMac/postgresl/var-9.4'
echo 'Also check in Postgresl Preferences that Postgres is running on Port 5444'
echo

#$PGSQL -D $PGDATA -l $PGLOG -w start
