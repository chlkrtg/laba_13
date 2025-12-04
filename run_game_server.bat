@echo off
echo Launching match game server...
mvn exec:java -Dexec.mainClass=ex3.GameServer
pause