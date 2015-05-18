@echo off
title Stop Glassfish
cls

:: set default code page
chcp 1252 > nul

:: find base directories
for %%? in ("%~dp0..\glassfish") do set APPBASE=%%~f?

cd "%APPBASE%\bin"
".\asadmin" stop-domain geoserver