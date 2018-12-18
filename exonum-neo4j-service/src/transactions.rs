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

///Transaction groups
transactions! {
    /// Our neo4j Transaction group.
    pub Neo4JTransactions {
        const SERVICE_ID = NEO4J_SERVICE_ID; // Remove this when updating.
        /// Commit set of queries as a single transaction in neo4j database
        struct CommitQueries {
            ///Queries for the transaction
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
                let description = format!("Database error: {}", error.msg());
                ExecutionError::with_description(1 as u8, description)
            },
            Error::PossibleConnectionError(error) => {
                let description = format!("Possible connection error: {}", error.msg());
                ExecutionError::with_description(2 as u8, description)
            }
        }

    }
}

impl Transaction for CommitQueries {
    fn verify(&self) -> bool {
        /*let hash = self.hash();
        println!("Verifying!");
        let req = getProtoTransactionRequest(self.queries(), "hahshashhash");
        //TODO implement getting neo4J server info from conf somehow.
        let client = getClient(9994);
        let resp = client.verify(RequestOptions::new(), req);
        let answer = resp.wait();
        let mut verified = false;

        let result = match answer {
            Ok(x) => {
                match x.1.get_result() {
                    Status::SUCCESS => {verified = true; Ok(())},
                    Status::FAILURE => {
                        //let error = x.1.get_error();
                        verified = false;
                        Err(Error::DataBaseError(error.get_message()))
                    }
                }
            },
            Err(_) => {
                verified = false;
                Err(Error::PossibleConnectionError(format!("{:?}", e)))
            }
        };
        match result {
            Ok(_) => {},
            Err(e) => {

            }
        }
        println!("Verified value is {}", verified);
        verified*/
        true //TODO maybe implement the signature checking since it is mandatory for .9, if we stay in this version.
    }

    fn execute(&self, fork: &mut Fork) -> ExecutionResult {
        let hash = self.hash();

        let mut schema: Schema<&mut Fork> = Schema::new(fork);

        let q = Queries::new(self.queries(), &hash, "");

        let node_changes : ExecuteResponse = q.execute(&mut schema);
        let result : ExecutionResult = match node_changes {
            ExecuteResponse::Changes(node_changes) => {
                for nc in node_changes{
                    for uuid in nc.get_uuis(){
                        schema.add_node_history(uuid, &nc)
                    }
                }
                schema.add_query(q);
                Ok(())
            },
            ExecuteResponse::Error(e) => {
                println!("We got error {}", e.msg());
                let q = Queries::new(self.queries(), &hash, format!("We got error: {}", e.msg()).as_str());
                schema.add_query(q);
                Ok(())
            }
        };

        result
    }
}