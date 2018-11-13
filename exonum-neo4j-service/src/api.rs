
use exonum::{
    api::{self, ServiceApiBuilder, ServiceApiState},
    crypto::{Hash}
};
use test_value::TestValue;
use schema::Schema;

/// Describes the query parameters for the `get_wallet` endpoint.
encoding_struct! {
    struct NewValueQuery {
        /// Public key of the queried wallet.
        name: &str,
    }
}

/// Response to an incoming transaction returned by the REST API.
#[derive(Debug, Serialize, Deserialize)]
pub struct TransactionResponse {
    /// Hash of the transaction.
    pub tx_hash: Hash,
}

/// Public service API description.
#[derive(Debug, Clone)]
pub struct TestApi;


impl TestApi {

    /// Endpoint for dumping all wallets from the storage.
    pub fn get_values(state: &ServiceApiState, _query: ()) -> api::Result<Vec<TestValue>> {
        let snapshot = state.snapshot();
        let schema = Schema::new(snapshot);
        let idx = schema.values();
        let values = idx.values().collect();
        Ok(values)
    }

    /// 'ServiceApiBuilder' facilitates conversion between transactions/read requests and REST
    /// endpoints; for example, it parses `POST`ed JSON into the binary transaction
    /// representation used in Exonum internally.
    pub fn wire(builder: &mut ServiceApiBuilder) {
        // Binds handlers to specific routes.
        builder
            .public_scope()
            .endpoint("v1/values", Self::get_values);
            //.endpoint_mut("v1/values", Self::post_transaction);
    }
}