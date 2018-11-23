#![allow(bare_trait_objects)]

use exonum::{
    blockchain::{ExecutionResult, Transaction},
    storage::{Fork},
    crypto::{CryptoHash}
};

use schema::Schema;
use structures::getProtoBufList;
use structures::Queries;
use NEO4J_SERVICE_ID;
//use std::io::{self, Write};



transactions! {
    /// Transaction group.
    pub Neo4JTransactions {
        const SERVICE_ID = NEO4J_SERVICE_ID; // Remove this when updating.
        // Transfer `amount` of the currency from one wallet to another.
        struct CommitQueries {
            queries: &str,
        }
    }
}

impl Transaction for CommitQueries {
    fn verify(&self) -> bool {
        let protoFields = getProtoBufList(self.queries());
        true
    }

    fn execute(&self, fork: &mut Fork) -> ExecutionResult {
        let hash = self.hash();

        let mut schema = Schema::new(fork);
        let queries = self.queries();
        let q = Queries::new(queries);
        q.execute();

        schema.add_query(q, &hash);

        Ok(())
    }
}