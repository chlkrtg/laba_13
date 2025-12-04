@echo off
echo Launching calculator server...
mvn exec:java -Dexec.mainClass=ex1.CalculatorServer
pause