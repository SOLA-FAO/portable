@echo off
title Start Desktop
cls

:: set default code page
chcp 1252 > nul

:: find base directories
for %%? in ("%~dp0..\") do set APPBASE=%%~f?

"%APPBASE%\jdk\jre\bin\javaw.exe" -jar "%APPBASE%\clients\sola-desktop.jar"
