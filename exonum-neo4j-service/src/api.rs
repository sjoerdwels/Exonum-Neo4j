
use exonum::{
    api::{self, ServiceApiBuilder, ServiceApiState},
    crypto::{Hash},
    blockchain::Transaction,
    node::TransactionSend,
};
use std::string::String;

use structures::{Queries};
use schema::Schema;

use transactions::Neo4JTransactions;

/// Describes the query parameters for the `get_wallet` endpoint.
encoding_struct! {
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
}
encoding_struct! {
    struct NodeHistoryQuery {
        node_name: &str,
    }
}

/// Public service API description.
#[derive(Debug, Clone)]
pub struct Neo4JApi;


impl Neo4JApi {

    /// Endpoint for dumping all wallets from the storage.
    pub fn get_queries(state: &ServiceApiState, _query: ()) -> api::Result<Vec<Queries>> {
        print!("Collecting queries");
        let snapshot = state.snapshot();
        let schema = Schema::new(snapshot);
        let idx = schema.queries();
        let values = idx.values().collect();
        Ok(values)
    }

    pub fn get_node_history(state: &ServiceApiState, query: NodeHistoryQuery) -> api::Result<Vec<String>> {
        let snapshot = state.snapshot();
        let schema = Schema::new(snapshot);
        let idx = schema.node_history(query.node_name());
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

        state.sender().send(transaction)?;
        Ok(CommitResponse { tx_hash })
    }

    /// 'ServiceApiBuilder' facilitates conversion between transactions/read requests and REST
    /// endpoints; for example, it parses `POST`ed JSON into the binary transaction
    /// representation used in Exonum internally.
    pub fn wire(builder: &mut ServiceApiBuilder) {
        // Binds handlers to specific routes.
        builder
            .public_scope()
            .endpoint("v1/queries", Self::get_queries)
            .endpoint("v1/node_history", Self::get_node_history)
            .endpoint_mut("v1/queries", Self::post_transaction);
    }
}