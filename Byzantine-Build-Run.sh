# This script is used to compile, run the server processes (4 total process, 3 are lieutenants, 1 commander) 

# Assumptions:
# -----------
# 1. Maven is installed (http://maven.apache.org/), mvn command is available in PATH variable
# 2. Java is installed (https://java.com), java command is available in PATH variable
# 3. You should be connected to internet, as Maven will download the dependent libraries for compilation.

# Compile the java classes
mvn clean install -DskipTests

# Run the processes (including RMI registry)
java -Djava.security.policy=./target/classes/.java.policy -jar ./target/Byzantine-1.0.0.jar

