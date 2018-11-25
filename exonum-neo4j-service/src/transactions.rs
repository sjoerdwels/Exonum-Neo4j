#![allow(bare_trait_objects)]

use exonum::{
    blockchain::{ExecutionResult, Transaction},
    storage::{Fork},
    crypto::{CryptoHash}
};

use schema::Schema;

use structures::getProtoBufList;
use structures::{NodeChange, Queries};
use NEO4J_SERVICE_ID;
//use std::io::{self, Write};



transactions! {
    /// Transaction group.
    pub TestTransactions {
        const SERVICE_ID = TEST_SERVICE_ID; // Remove this when updating.
        // Transfer `amount` of the currency from one wallet to another.
        struct ChangeValue {
            name: &str,
            amount:  u64,
        }

        struct NewValue {
            name: &str
        }
    }
}


impl Transaction for ChangeValue {
    fn verify(&self) -> bool {
        true
    }

    fn execute(&self, fork: &mut Fork) -> ExecutionResult {
        let hash = self.hash();

        let mut schema = Schema::new(fork);

        let queries = self.queries();
        let q = Queries::new(queries, &hash);
        let node_changes : Vec<NodeChange> = q.execute();

        schema.add_query(q);
        for nc in node_changes{
            schema.add_node_history(nc)
        }
        Ok(())
    }
}