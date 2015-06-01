#!/bin/bash

# The current directory where this command has been executed from
current_dir=`dirname $0`
#pushd "$dir" > /dev/null
cd ./.

base_dir=$(pwd)

postgres_dir="$base_dir/postgresql"

#PGSQL="$postgres_dir/App/PgSQL"
#PGDATA="$postgres_dir/Data/data"
PGLOG="$base_dir/logs/postgresql.log"
PGLOCALEDIR="$postgres_dir/App/PgSQL/share"
PGDATABASE=postgres
PGUSER=postgres

echo
echo Terminate db connections using PG Admin...
echo
#"$PGSQL\bin\psql" --port=5444 --command="SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE  pg_stat_activity.datname = 'sola' AND pid <> pg_backend_pid();"

echo Quit Postgres using Postgres icon in the menu bar at the top of the screen...
#"$PGSQL\bin\pg_ctl" -D "$PGDATA" stop