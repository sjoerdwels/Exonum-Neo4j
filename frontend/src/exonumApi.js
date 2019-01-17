const Exonum = require('exonum-client');
const axios = require('axios');

const TX_URL = '/api/services/neo4j_blockchain/v1/';
const TX_EXPLORER_URL = 'api/services/neo4j_blockchain/v1/transaction?hash_string=';
const PROTOCOL_VERSION = 0;
const SERVICE_ID = 144;
const TX_INSERT_QUERIES_ID = 0;
const ATTEMPTS = 10;
const ATTEMPT_TIMEOUT = 500;

// Messages
const insertQueries = Exonum.newMessage({
    protocol_version: PROTOCOL_VERSION,
    service_id: SERVICE_ID,
    message_id: TX_INSERT_QUERIES_ID,
    fields: [
        {name: 'queries', type: Exonum.String},
        {name: 'datetime', type: Exonum.String},
    ]
});

// Keypairs
const keyPair = {
    publicKey: process.env.EXONUM_PUBLIC_KEY,
    secretKey: process.env.EXONUM_PRIVATE_KEY
};

function sendTransaction(query) {

    const data = {
        queries: query,
        datetime: Date.now().toString()
    };

    const signature = insertQueries.sign(keyPair.secretKey, data);

    return insertQueries.send(TX_URL  + "insert_transaction", TX_EXPLORER_URL, data, signature, ATTEMPTS, ATTEMPT_TIMEOUT)
}


function getNodeHistory(uuid) {
    return axios.get(TX_URL  + `node_history?node_uuid=${uuid}`).then(response => response.data)
}

function getTransaction(hash) {
    return axios.get(TX_URL  +`transactions?hash_string=${hash}`).then(response => response.data);
}

exports.sendTransaction = sendTransaction;
exports.getNodeHistory = getNodeHistory;
exports.getTransaction = getTransaction;
