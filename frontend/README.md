# Modified Neo4j Movies Example Application - `neo4j-driver &  Exonum` Edition

// todo update image
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

When running in "dev" mode navigate to [http://localhost:8080/webpack-dev-server/](http://localhost:8080/webpack-dev-server/) to see the application.

After executing the `npm run build` command,  open local file "build/index.html" in your browser.
