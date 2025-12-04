@echo off
echo Launching calculator client...
mvn exec:java -Dexec.mainClass=ex1.CalculatorClient
pause