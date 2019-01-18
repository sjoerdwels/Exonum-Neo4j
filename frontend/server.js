const webpack = require('webpack');
const webpackMiddleware = require('webpack-dev-middleware');
const webpackConfig = require('./webpack.config.js');
const express = require('express');
const bodyParser = require('body-parser');
const dotenv = require('dotenv');

dotenv.config();

// Initialize application
let app = express();

// Get settings
const WEBSERVER_PORT = process.env.WEBSERVER_PORT;
const EXONUM_ADDRESS = process.env.EXONUM_ADDRESS;
const EXONUM_PORT= process.env.EXONUM_PORT;
const EXONUM_API  = "http://" + EXONUM_ADDRESS +  ":"  + EXONUM_PORT;
const NEO4J_ADDRESS= process.env.NEO4J_BOLT_ADDRESS;
const NEO4J_PORT= process.env.NEO4J_BOLT_PORT;
const NEO4J_USERNAME= process.env.NEO4J_USERNAME;
const NEO4J_PASSWORD= process.env.NEO4J_PASSWORD;

// PRINT
console.log(`
Starting Exonum Neo4j Movies demo.
===================================
WEBSERVER_PORT = ${WEBSERVER_PORT} 
EXONUM_ADDRESS = ${EXONUM_ADDRESS} 
EXONUM_PORT = ${EXONUM_PORT} 
EXONUM_API =  ${EXONUM_API}
NEO4J_ADDRESS = ${NEO4J_ADDRESS} 
NEO4J_PORT = ${NEO4J_PORT} 
NEO4J_USERNAME = ${NEO4J_USERNAME} 
NEO4J_PASSWORD = ${NEO4J_PASSWORD} 
`);

app.set('apiRoot', EXONUM_API);

// Configure parsers
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: true}));

// Set path to static files
app.use(express.static(__dirname + '/'));

// Activate routers
const api = require('./routes/api');
app.use('/api', api);

// Use webpack
app.use(webpackMiddleware(webpack(webpackConfig)));

app.listen(WEBSERVER_PORT);