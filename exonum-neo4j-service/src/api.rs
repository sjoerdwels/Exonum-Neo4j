
use exonum::{
    crypto::{Hash}
};

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