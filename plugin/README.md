# Neo4j Transaction manager

The Neo4j transaction manager for Exonum allows Neo4j transactions to be verified and executed.
For the first, the queries in the provided will be executed in a Neo4j transaction and be rolled back afterwards. The latter
commits the transaction such that the changes are final. In both cases, a list of database changes will be provided.

Please note that the transaction manager does work with other Neo4j Transaction Event Handlers.

#### Requirements
- Apache Maven

#### Compilation
Compilation of the Neo4j unmanaged extension ```mvn compile``` and ```mvn test-compile```

The compiled artefacts can be found in the **target/**  folder.

#### Neo4j installation

https://neo4j.com/docs/operations-manual/current/installation/

You can see an example of how we installed neo4j in the ../docker/DockerFile for ubuntu.

#### Extension building:
- Install Maven with "sudo apt-get install maven"
- In project repository root directory run "mvn package"
- Add created extension .jar file (in traget directory) to  ./var/lib/neo4j/plugins directory
- Start Neo4j by "sudo neo4j console"

#### Notes
The plugin will prevent external access to make changes to the neo4j database. Changes can only be made using the gRPC calls implemented in the plugin. Read requests can be done as usual.

