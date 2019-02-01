const Exonum = require('exonum-client');
const axios = require('axios');

const API_URL = '/api/services/neo4j_blockchain/v1/';
const EXPLORER_URL = '/api/explorer/v1/';

const PROTOCOL_VERSION = 0;
const SERVICE_ID = 144;
const TX_INSERT_QUERIES_ID = 0;
const ATTEMPTS = 0;
const ATTEMPT_TIMEOUT = 500;

const insertQueries = Exonum.newMessage({
    protocol_version: PROTOCOL_VERSION,
    service_id: SERVICE_ID,
    message_id: TX_INSERT_QUERIES_ID,
    fields: [
        {name: 'queries', type: Exonum.String},
        {name: 'datetime', type: Exonum.String},
        {name: 'pub_key', type: Exonum.PublicKey},
    ]
});

const keyPair = {
    publicKey: process.env.EXONUM_PUBLIC_KEY,
    secretKey: process.env.EXONUM_PRIVATE_KEY
};

function sendTransaction(query) {

    const data = {
        queries: query,
        datetime: Date.now().toString(),
        pub_key : process.env.EXONUM_PUBLIC_KEY
    };

    const signature = insertQueries.sign(keyPair.secretKey, data);

    insertQueries.signature = signature;

    let hash = insertQueries.hash(data);

    return insertQueries.send(API_URL  + "insert_transaction", EXPLORER_URL  +`transactions?hash=`, data, signature, ATTEMPTS, ATTEMPT_TIMEOUT).then(() => {
        return { tx_hash : hash }
    })
}

function getNodeHistory(uuid) {
    return axios.get(API_URL  + `node_history?node_uuid=${uuid}`).then(response => response.data)
}

function getTransaction(hash) {
    return axios.get(API_URL +`transaction?hash_string=${hash}`).then(response => response.data);
}

function getBlocks(count, latest, skipEmpty) {

    let suffix = ''
    if (!isNaN(parseInt(latest))) {
        suffix += '&latest=' + latest
    }

    if (skipEmpty) {
        suffix += '&skip_empty_blocks=true'
    }

    return axios.get(EXPLORER_URL +  `blocks?count=${count}${suffix}` ).then(response => response.data);
}

function getBlock(height) {
    return axios.get(EXPLORER_URL + `block?height=${height}`).then(response => response.data);
}

exports.sendTransaction = sendTransaction;
exports.getNodeHistory = getNodeHistory;
exports.getTransaction = getTransaction;
exports.getBlocks = getBlocks;
exports.getBlock = getBlock;
