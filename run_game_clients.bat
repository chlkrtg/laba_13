@echo off
echo Launching match clients...
start mvn exec:java -Dexec.mainClass=ex3.GameClient -Dexec.args="Игрок 1"
timeout /t 2 >nul
mvn exec:java -Dexec.mainClass=ex3.GameClient -Dexec.args="Игрок 2"