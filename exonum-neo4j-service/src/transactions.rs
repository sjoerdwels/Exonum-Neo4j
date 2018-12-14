#![allow(bare_trait_objects)]
#![allow(warnings)]


use exonum::{
    blockchain::{ExecutionResult, Transaction, ExecutionError},
    storage::{Fork},
    crypto::{CryptoHash},
};

use schema::Schema;

use structures::getProtoTransactionRequest;
use structures::{NodeChange, Queries, ExecuteResponse, ErrorMsg};
use NEO4J_SERVICE_ID;
use gRPCProtocol::Status;
use gRPCProtocol_grpc::{getClient, TransactionManager};
use grpc::RequestOptions;
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

/// Error codes emitted by wallet transactions during execution.
#[derive(Debug, Fail)]
#[repr(u8)]
pub enum Error {
    ///Database error
    #[fail(display = "Database throws error on transaction")]
    DataBaseError(ErrorMsg),
    ///Database error
    #[fail(display = "Possible connection error with database")]
    PossibleConnectionError(ErrorMsg),
}

impl From<Error> for ExecutionError {
    fn from(value: Error) -> ExecutionError {
        match value {
            Error::DataBaseError(error) => {
                let description = format!("{}", error.msg());
                ExecutionError::with_description(1 as u8, description)
            },
            Error::PossibleConnectionError(error) => {
                let description = format!("{}", error.msg());
                ExecutionError::with_description(2 as u8, description)
            }
        }

    }
}

impl Transaction for CommitQueries {
    fn verify(&self) -> bool {
        println!("Verifying!");
        let req = getProtoTransactionRequest(self.queries(), "hahshashhash");
        //TODO implement getting neo4J server info from conf somehow.
        let client = getClient(9994);
        let resp = client.verify(RequestOptions::new(), req);
        let answer = resp.wait();
        let mut verified = false;
        match answer {
            Ok(x) => {
                match x.1.get_result() {
                    Status::SUCCESS => verified = true,
                    Status::FAILURE => {
                        //let error = x.1.get_error();
                        verified = false;
                        //Err(Error::DataBaseError(error.get_message()))?
                    }
                }
            },
            Err(_) => {
                verified = false;
                //Err(Error::PossibleConnectionError(format!(" {:?}", e)));
            }
        }
        println!("Verified value is {}", verified);
        verified
    }

    fn execute(&self, fork: &mut Fork) -> ExecutionResult {
        let hash = self.hash();

        let mut schema = Schema::new(fork);

        let queries = self.queries();
        let q = Queries::new(queries, &hash);

        let node_changes : ExecuteResponse = q.execute();//TODO alternate sequence of action for failed stuff.
        schema.add_query(q);
        match node_changes{
            ExecuteResponse::Changes(node_changes) => {
                for nc in node_changes{
                    for uuid in nc.get_uuis(){
                        schema.add_node_history(uuid, &nc)
                    }
                }
                Ok(())
            },
            ExecuteResponse::Error(e) => {
                println!("We got error {}", e.msg());

                Err(Error::DataBaseError(e))?
            }
        }


    }
}