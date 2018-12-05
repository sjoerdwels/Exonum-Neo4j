#![allow(bare_trait_objects)]
#![allow(warnings)]

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
        let q = Queries::new(queries, &hash);
        let node_changes : Vec<NodeChange> = q.execute();


        schema.add_query(q);
        for nc in node_changes{
            for uuid in nc.get_uuis(){
                schema.add_node_history(uuid, &nc)
            }
        }
        Ok(())
    }
}