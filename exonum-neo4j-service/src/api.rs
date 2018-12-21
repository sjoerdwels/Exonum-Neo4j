
//! neo4j exonum integration API. Provides us with necessary endpoints.

use exonum::{
    api::{self, ServiceApiBuilder, ServiceApiState},
    crypto::{Hash},
    blockchain::{Transaction},
    node::TransactionSend,
    encoding::serialize::FromHex,
};

use structures::{Queries};
use schema::Schema;
use transactions::Neo4JTransactions;

use std::io;
use std::string::String;

/// Describes the query parameters for the `insert_transaction` endpoint.
encoding_struct! {
    ///Commit queries transaction.
    struct commitQueriesQuery {
        /// Public key of the queried wallet.
        queries: &str,
    }
}

/// Response to an incoming commit request returned by the REST API.
#[derive(Debug, Serialize, Deserialize)]
pub struct CommitResponse {
    /// Hash of the transaction.
    pub tx_hash: Hash,
    ///error msg, if none, is empty
    pub error_msg: std::string::String,
}
///Node history query
encoding_struct! {
    ///Node history query
    struct NodeHistoryQuery {
        ///node's uuid
        node_uuid: &str,
    }
}

///Query query
encoding_struct! {
    ///Node history query
    struct GetQueryQuery {
        ///node's uuid
        hash_string: &str,
    }
}

/// Public service API description.
#[derive(Debug, Clone)]
pub struct Neo4JApi;


impl Neo4JApi {

    /// Endpoint for dumping all queries from the storage.
    pub fn get_queries(state: &ServiceApiState, _query: ()) -> api::Result<Vec<Queries>> {
        print!("Collecting queries");
        let snapshot = state.snapshot();
        let schema = Schema::new(snapshot);
        let idx = schema.queries();
        let values = idx.values().collect();
        Ok(values)
    }

    /// Returns query based on provided hash.
    pub fn get_query(state: &ServiceApiState, query: GetQueryQuery) -> api::Result<Queries> {
        let snapshot = state.snapshot();
        let schema = Schema::new(snapshot);
        match Hash::from_hex(query.hash_string()) {
            Ok(query_hash) => {
                let query = schema.query(&query_hash);
                match query {
                    Some(x) => Ok(x),
                    None => Err(api::Error::from(io::Error::new(io::ErrorKind::Other, "No query found"))),
                }
            },
            Err(e) => Err(api::Error::from(io::Error::new(io::ErrorKind::Other, format!("Error unpacking hash: {:?}", e)))),
        }
    }

    /// Endpoint for getting a single node's history by providing it's uuid.
    pub fn get_node_history(state: &ServiceApiState, query: NodeHistoryQuery) -> api::Result<Vec<String>> {
        let snapshot = state.snapshot();
        let schema = Schema::new(snapshot);
        let idx = schema.node_history(query.node_uuid());
        let mut values = Vec::new();
        for val in idx.iter(){
            values.push(format!("{}", val));
        }
        Ok(values)
    }


    
    /// Common processing for transaction-accepting endpoints.
    pub fn post_transaction(
        state: &ServiceApiState,
        query: Neo4JTransactions,
    ) -> api::Result<CommitResponse> {
        let transaction: Box<dyn Transaction> = query.into();
        let tx_hash = transaction.hash();
        println!("tx_hash is {}", tx_hash.to_string());

        match state.sender().send(transaction) {
            Ok(()) =>   Ok(CommitResponse { tx_hash: tx_hash, error_msg: format!("") }),
            Err(err) => Ok(CommitResponse { tx_hash: tx_hash, error_msg: format!("got error: {:?}", err) })
        }
    }

    /// 'ServiceApiBuilder' facilitates conversion between transactions/read requests and REST
    /// endpoints; for example, it parses `POST`ed JSON into the binary transaction
    /// representation used in Exonum internally.
    pub fn wire(builder: &mut ServiceApiBuilder) {
        // Binds handlers to specific routes.
        builder
            .public_scope()
            .endpoint("v1/transactions", Self::get_queries)
            .endpoint("v1/node_history", Self::get_node_history)
            .endpoint("v1/transaction", Self::get_query)
            .endpoint_mut("v1/insert_transaction", Self::post_transaction);
    }
}