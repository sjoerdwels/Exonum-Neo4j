# Neo4j-Exonum Demo

## Introduction

The Neo4j & Exonum Demo Application is an example application based on the existing [Neo4j Movie Database](https://neo4j.com/developer/example-project/) example. 
It's an simple one page app that uses the bolt javascript driver to connect to an Neo4j database. In the original version you could:
* movie search by title
* single movie listing
* graph visualization of the domain

##### Additions & Modifications
The original demo application used the Neo4j’s movie demo database (movie, actor, director) as data set. This dataset has a set of movies and actors which are linked to each other. 
As in our application, all nodes need to agree to the transactions, we can not use this data set and have to insert the data by our self. Also, we would like to query a nodes history.

Therefore, we added the following features:
* Insert transactions
* Retrieve blockchain blocks, transactions and transaction status.
* Retrieve node history by UUID.


##### Start the application
Use the readme file in the docker directory to start the application, then continue with the next chapter.

### Our first query
Neo4j uses cypher as a query language, which used a mix of SQL keywords and Cypher and ASCII-Art for patterns.

The Movie demo uses the following domain model:
``` cypher
(:Person {name})-[:ACTED_IN {roles}]->(:Movie {title,released})
```

Now open the frontend on [Node 1](http://localhost:3001), you can see that there are no movies in the database. We are going to insert our first movies with actors by simply executing the following query:

``` cypher
CREATE (TheMatrix:Movie {title:'The Matrix', released:1999, tagline:'Welcome to the Real World'})
CREATE (Keanu:Person {name:'Keanu Reeves', born:1964})
CREATE (Carrie:Person {name:'Carrie-Anne Moss', born:1967})
CREATE (Laurence:Person {name:'Laurence Fishburne', born:1961})
CREATE (Hugo:Person {name:'Hugo Weaving', born:1960})
CREATE (LillyW:Person {name:'Lilly Wachowski', born:1967})
CREATE (LanaW:Person {name:'Lana Wachowski', born:1965})
CREATE (JoelS:Person {name:'Joel Silver', born:1952})
CREATE
  (Keanu)-[:ACTED_IN {roles:'Neo'}]->(TheMatrix),
  (Carrie)-[:ACTED_IN {roles:'Trinity'}]->(TheMatrix),
  (Laurence)-[:ACTED_IN {roles:'Morpheus'}]->(TheMatrix),
  (Hugo)-[:ACTED_IN {roles:'Agent Smith'}]->(TheMatrix),
  (LillyW)-[:DIRECTED]->(TheMatrix),
  (LanaW)-[:DIRECTED]->(TheMatrix),
  (JoelS)-[:PRODUCED]->(TheMatrix)
```

There are some things to notice  here:
1. The frontend is 'pulling' the blockchain network to receive information about the transaction. When it receives the transaction information, it will be shown in the 'Transaction Information' block on the webpage.
2. The transaction has a status which could change. By clicking on the 'Hash', the frontend application will reload the transaction status by pulling 
the transaction from the blockchain framework again.

Make sure that the query status is 'SUCCESS' before you continue.

#### Search for the movie
Now you can use the search to find 'The Matrix' movie. As you can see, every node has it owns UUID. Because the UUID is created by our application,
 you can recognize a pattern. Each created node in a transaction has the same prefix of the transcation ID. And every node has a
 suffix of a counter, which is the number of the change in the ordered list of changes retrieved from Neo4j.

#### Retrieve node history
Click on the UUID of 'The Matrix' movie to retrieve the change history of that node.

#### Add a Rate property to 'The Matrix' movie node.
We are going to add a new property 'rate' to the movie. 

```cypher
MATCH (n:Movie { title:'The Matrix' })
SET n.rate = 5
```
Now open the Node history and check that the change is added.

#### Remove the 'The Matrix' movie.
Try to remove 'The Matrix' movie using the following query:

```cypher
MATCH (n:Movie { title:'The Matrix' })
DELETE n
```
We expect the query to fail, because it should not be allowed to delete a node which is in a relationship. The 'consistency' property of an Neo4j transaction fails,
as the database is not consistent after the transaction is executed. Therefore, an error will return but it is not related to an specific query.

#### Modify UUID property of a node
Try to modify a UUID property of a node.

```cypher
MATCH (n:Movie { title:'The Matrix' })
set n.uuid = 'changed_uuid'
```
Again, we expect the query to fail because it should not be possible to modify the UUID of a node. 

#### The Neo4j graph
Open the 'Neo4j movies graph tab' and reload the graph by clicking on the right top button.
 The reload button will retrieve all the nodes with label Movie and Actors and their relations directly form Neo4j using the javascript bolt driver.

Here you can see the Movies (black), and actors (green) in a graph. By clicking on the graph, you can retrieve the Node History again.

#### Neo4j statistics
Because it is really hard to compare databases, we created some easy calculated statistics about all the nodes.
Now open [Node 2](http://localhost:3002), and compare the statistics, compare the node history, it is all the same.

#### Blocks
When you start the demo, the Exonum validator nodes will create blocks and these ledger in the background. These block do not 
 necessary contain transactions. In the frontend, it is possible to retrieve these blocks and their transaction hashes.

Open the 'Block' tab and click on the first block with the lowest height that is including a transaction. If the first transaction that you inserted is the 'The Matrix' movie, you will see 
it's transactions hash on that.

Notice that, in the next block(s) there are 4 new transactions. These are the 4 'audit_block' transactions. An audit block transaction will audit the blocks which are not audited yet. In other words, all the changes of Neo4j which are not recorded on the blockchain yet will now be auditted.

The assumption was that, when a transaction is the same, it will be treated the same. But this only holds for a single node, as each nodes signs the 'audit_block' message with its
own hash. Therefore each validator creates it owns audit_block transaction resulting in message overhead. A simple solution to solve this problem is to create a single shared public-private key pair that is used for the
audit block transaction. 

### Neo4j browser
Open the [Neo4j browser](http://localhost:7471) on Node 1 and login.

Username:
```bash
localhost:7671
```
Password:
```bash
exonumNeo4j
```

#### Retrieve all nodes and relationships

Now you are directly connected to the Neo4j database and can retrieve all the nodes using the following query:
```cypher
MATCH (n) RETURN n
```

Learn how to use the Neo4j browser for querying, visualizing and interacting with your graph [here](https://neo4j.com/developer/guide-neo4j-browser/).

#### Create a new node
Try to create a new node by executing the following query:
```cypher
CREATE (n) RETURN n
```
Once again, this should fail, as it is not allowed to execute queries directly from Neo4j.