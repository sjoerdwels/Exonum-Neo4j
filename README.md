# Exonum-Neo4j
Neo4j and Exonum integration

# Requirements
- The Rust package manager Cargo.
- Apache Maven

## Compilation
Compilation of the Exonum Service ```cargo build```
Compilation of the Neo4j unmanaged extension ```mvn compile``` and ```mvn test-compile```

The compiled artefacts can be found in the **target/**  folder.

## Installation
Ubuntu Linux 14.04 and newer instructions:
Neo4j installation:
- install Java 8 and set it as default JDK with following commands
    - sudo add-apt-repository ppa:webupd8team/java
    - sudo apt-get update
    - sudo apt-get install oracle-java8-installer
    - update-java-alternatives --list               (This shows <java8name> value for next command)
    - sudo update-java-alternatives --jre --set <java8name> 
 
- Add correct repository and the install Neo4j
    - wget -O - https://debian.neo4j.org/neotechnology.gpg.key | sudo apt-key add -
    - echo 'deb https://debian.neo4j.org/repo stable/' | sudo tee -a /etc/apt/sources.list.d/neo4j.list
    - sudo apt-get update
    - sudo apt-get install neo4j=1:3.4.9

Extension building:
- Install Maven with "sudo apt-get install maven"
- In project repository root directory run "mvn package"
- Add created extension .jar file (in traget directory) to  ./var/lib/neo4j/plugins directory
- Start Neo4j by "sudo neo4j console"

## Tests

## Contributors
Indrek Klangberg
Silver Vapper
Sjoerd Wels
 
## License
