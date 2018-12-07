#![allow(bare_trait_objects)]
#![allow(warnings)]

extern crate grpc;

use exonum::{
    blockchain::{ExecutionResult, Transaction},
    storage::{Fork},
    crypto::{CryptoHash}
};

use schema::Schema;

use structures::getProtoTransactionRequest;
use structures::{NodeChange, Queries};
use NEO4J_SERVICE_ID;
use gRPCProtocol::Status;
use gRPCProtocol_grpc::{getClient, TransactionManager};
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
        let req = getProtoTransactionRequest(self.queries(), "");
        //TODO implement getting neo4J server info from conf somehow.
        let client = getClient(50051);
        let resp = client.verify_transaction(grpc::RequestOptions::new(), req);
        let answer = resp.wait();
        let mut verified = false;
        match answer {
            Ok(x) => {
                match x.1.get_result() {
                    Status::SUCCESS => verified = true,
                    Status::FAILURE => verified = false //TODO must raise proper error to client, what went wrong
                }
            },
            _ => {verified = false } //TODO must raise proper error to client! Understand Error of result.
        }
        println!("Verified value is {}", verified);
        verified
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