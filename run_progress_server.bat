@echo off
echo Launching progress-bar server...
mvn exec:java -Dexec.mainClass=ex2.ProgressServer
pause