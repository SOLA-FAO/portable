@echo off
title PostgreSQL 9.4.1
cls

:: set default code page
chcp 1252 > nul

:: find base directories
for %%? in ("%~dp0..\postgresql") do set APPBASE=%%~f?
for %%? in ("%~dp0..\") do set BASE=%%~f?

:: set up postgres variables
set PGSQL=%APPBASE%\App\PgSQL
set PGDATA=%APPBASE%\Data\data
set PGLOG=%BASE%\logs\postgresql.log
set PGLOCALEDIR=%PGSQL%\share\
set PGDATABASE=postgres
set PGUSER=postgres
set PATH=%PGSQL%\bin;%PATH%


echo Terminating db connections...
"%PGSQL%\bin\psql" --port=5444 --command="SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE  pg_stat_activity.datname = 'sola_sl' AND pid <> pg_backend_pid();"

echo Stopping database...
"%PGSQL%\bin\pg_ctl" -D "%PGDATA%" stop