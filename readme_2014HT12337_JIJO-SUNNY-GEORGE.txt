======================
A. Byzantine-Consensus
======================
B. Assumptions:
--------------
# 1. Maven is installed (http://maven.apache.org/), mvn command is available in PATH variable.
# 2. Java is installed (https://java.com), java command is available in PATH variable.
# 3. You should be connected to internet, as Maven will download the dependent libraries for compilation.
=======================
C. How to Build and Run:
-----------------------
# 1. In the console, please navigate to the 'Byzantine' directory. It will have 'Byzantine-Build-Run.sh' script file.
# 2. Run the following command to get the executable class files:
     ./Byzantine-Build-Run.sh

# 3. The above command will compile the java classes, run the RMI Registry, run 4 server processes (those act both as server and client).
# 4. These processes will have indexes as : 0 - Commander, 1,2,3 - Lieutenants. Please see the log messages for the activities happening at each process end.

====================================
D. Run the Tests
====================================
# 1. Open another console.
# 2. Please navigate to the 'Byzantine' directory (same as in the step to Build and Run).
# 3. Run command as following:
     mvn test

# 4. This will run two test scenarios. The first scenario has maximum traitor count as 1 (Order=ATTACK), the second test has max traitor count as 1 (Order=RETREAT).
# 5. Monitor the other console, where all the processes were running.

====================================
E. The output of Step C and D 
====================================
# 1. The output of these 2 steps are included in files output1.txt and output2.txt respectively.