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

echo Starting database....
"%PGSQL%\bin\pg_ctl" -D "%PGDATA%" -l "%PGLOG%" -w start
