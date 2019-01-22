# Neo4j & Exonum Demo Application

![image of application](./img/demo.png)

## Stack

* [Neo4j Bolt JavaScript Driver](https://github.com/neo4j/neo4j-javascript-driver)
* Neo4j
* Exonum
* Frontend: ES6, jquery, bootstrap, [d3.js](http://d3js.org/)
* Webpack for building web assets

## Quickstart

### Setup

```bash
$ npm install
```

### Run locally

* Start the Exonum service and Neo4j database
* Setup settings
* Run the App inside the Webpack Dev Server:

```bash
# run in developement mode (refreshes the app on source code changes)
$ npm run dev

# builds the release version with client assets in "build" directory
$ npm run build
```

Test query:
```cypher
CREATE (TheMatrix:Movie {title:'The Matrix', released:1999, tagline:'Welcome to the Real World'})
CREATE (Keanu:Person {name:'Keanu Reeves', born:1964})
CREATE (Carrie:Person {name:'Carrie-Anne Moss', born:1967})
CREATE (Laurence:Person {name:'Laurence Fishburne', born:1961})
CREATE (Hugo:Person {name:'Hugo Weaving', born:1960})
CREATE (LillyW:Person {name:'Lilly Wachowski', born:1967})
CREATE (LanaW:Person {name:'Lana Wachowski', born:1965})
CREATE (JoelS:Person {name:'Joel Silver', born:1952})
CREATE
  (Keanu)-[:ACTED_IN {roles:['Neo']}]->(TheMatrix),
  (Carrie)-[:ACTED_IN {roles:['Trinity']}]->(TheMatrix),
  (Laurence)-[:ACTED_IN {roles:['Morpheus']}]->(TheMatrix),
  (Hugo)-[:ACTED_IN {roles:['Agent Smith']}]->(TheMatrix),
  (LillyW)-[:DIRECTED]->(TheMatrix),
  (LanaW)-[:DIRECTED]->(TheMatrix),
  (JoelS)-[:PRODUCED]->(TheMatrix)
```
When running in "dev" mode navigate to [http://localhost:8080/webpack-dev-server/](http://localhost:8080/webpack-dev-server/) to see the application.

After executing the `npm run build` command,  open local file "build/index.html" in your browser.
