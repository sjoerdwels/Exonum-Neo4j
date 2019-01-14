#![allow(bare_trait_objects)]
#![allow(warnings)]
/// Transaction documentation
use exonum::{
    blockchain::{Schema as CoreSchema, ExecutionResult, Transaction, ExecutionError},
    storage::{Fork},
    crypto::{CryptoHash, Hash},
    encoding::serialize::FromHex,
    helpers::Height,
};

use schema::Schema;
use structures::{NodeChange, Neo4jTransaction, ErrorMsg};
use neo4j::{Neo4jRpc, ExecuteResponse, Neo4jConfig, generate_database_changes_from_proto};
use neo4j::transaction_manager::{Status, BlockChangesResponse};
use neo4j::ExecuteResponse::{ChangeResponse, Error as DBError};

use NEO4J_SERVICE_ID;

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

        ///Retrieves all changes from Neo4j that are supposed to be executed.
        struct AuditBlocks {
            block_id: &str,
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

impl From<ErrorMsg> for ExecutionError {
    fn from(error: ErrorMsg) -> ExecutionError {
        let description = format!("{}", error.msg());
        ExecutionError::with_description(1 as u8, description)
    }
}



impl AuditBlocks {
    pub fn retrieve_changes_from_neo4j(&self, fork: &Fork) -> Vec<BlockChangesResponse>{
        let schema: Schema<&Fork> = Schema::new(fork);
        let core_schema : CoreSchema<&Fork> = CoreSchema::new(&fork);
        let all_blocks_by_height = core_schema.block_hashes_by_height();
        let all_blocks = core_schema.blocks();


        let mut returnVector : Vec<BlockChangesResponse> = Vec::new();

        let neo4j_config = Neo4jConfig{
            address : String::from("127.0.0.1"),
            port : 9994
        };
        let neo4j_rpc = Neo4jRpc::new(neo4j_config);

        let last_block_option = schema.get_last_confirmed_block();
        let mut last_block_index = 0;
        match last_block_option {
            Some(block_hash) => {
                let block_option = all_blocks.get(&block_hash);
                match block_option {
                    Some(block) => {
                        last_block_index = block.height().0+1;

                    },
                    None => {
                        //TODO should not get here
                    }
                }

            },
            None => {}
        }
        for x in last_block_index..all_blocks_by_height.len() {
            let added_block_hash = all_blocks_by_height.get(x);
            match added_block_hash {
                Some(block) => {
                    let mut ignore = true;
                    let h = Height(x);
                    let transactions = core_schema.block_transactions(h);
                    for trans in transactions.iter() {
                        match schema.neo4j_transaction(&trans) {
                            Some(_) => ignore = false,
                            None => {}
                        }
                    }
                    if ignore {
                        continue
                    }

                    match neo4j_rpc.retrieve_block_changes(block) {
                        ChangeResponse(changes) => { returnVector.push(changes) },
                        DBError(e) => {println!("{:?}", e.msg())},//TODO error handling
                        _ => {}
                    }
                },
                None => {} //TODO shouldn't get here but error handling.
            }

        }
        returnVector
    }

    pub fn add_changes_to_exonum(&self, fork: &mut Fork, changes_per_block : Vec<BlockChangesResponse>){
        let mut schema: Schema<&mut Fork> = Schema::new(fork);
        for block_changes in changes_per_block {
            for transaction_changes in block_changes.get_transactions() {
                match Hash::from_hex(transaction_changes.get_transaction_id()){
                    Ok(transaction_hash) => {
                        match transaction_changes.get_result() {
                            Status::SUCCESS => {
                                let changes = transaction_changes.get_modifications();
                                let change_vec: Vec<NodeChange> = generate_database_changes_from_proto(changes, &mut schema);
                                for nc in change_vec {
                                    for uuid in nc.get_uuis() {
                                        schema.add_node_history(uuid, &nc)
                                    }
                                }
                                schema.update_neo4j_transaction(&transaction_hash, "", "SUCCESS");
                            }
                            Status::FAILURE => {
                                let error = transaction_changes.get_error();
                                let failed_query = error.get_failed_query();
                                let error_msg = format!("{}\nHappened in query: {}\n{}", error.get_message(), failed_query.get_query(), failed_query.get_error());
                                schema.update_neo4j_transaction(&transaction_hash, error_msg.as_str(), "ERROR");
                            }
                        }
                    },
                    _ => {},
                }

            }
        }
    }

    pub fn update_last_block(&self, fork: &mut Fork){
        let last_block = {
            CoreSchema::new(&fork).block_hashes_by_height().last()
        };

        let mut schema: Schema<&mut Fork> = Schema::new(fork);
        match last_block {
            Some(hash) => {

                schema.set_last_confirmed_block(hash)
            },
            None => {}
        }
    }
}

impl Transaction for AuditBlocks {
    fn verify(&self) -> bool {
        true
    }

    fn execute(&self, fork: &mut Fork) -> ExecutionResult {
        let changes = self.retrieve_changes_from_neo4j(fork);
        self.add_changes_to_exonum(fork, changes);
        self.update_last_block(fork);
        Ok(())
    }
}

impl Transaction for CommitQueries {
    fn verify(&self) -> bool {
        true
    }

    fn execute(&self, fork: &mut Fork) -> ExecutionResult {

        let hash = self.hash();

        let mut schema: Schema<&mut Fork> = Schema::new(fork);

        let q = Neo4jTransaction::new(self.queries(), "", "PENDING");

        schema.add_neo4j_transaction(q, &hash);
        Ok(())
    }
}