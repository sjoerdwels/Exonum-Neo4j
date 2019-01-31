#![allow(bare_trait_objects)]
#![allow(warnings)]
/// Transaction documentation
use exonum::{
    blockchain::{ExecutionResult, Schema as CoreSchema, Transaction},
    crypto::{CryptoHash, Hash, PublicKey},
    encoding::serialize::FromHex,
    helpers::Height,
    messages::Message,
    storage::Fork,
};

use neo4j::proto::transaction_manager::{BlockChangesResponse, Status};
use neo4j::ExecuteResponse::{ChangeResponse, Error as DBError};
use neo4j::{generate_database_changes_from_proto, get_neo4j_rpc_client, ExecuteResponse};
use schema::Schema;
use structures::{ErrorMsg, Neo4jTransaction, NodeChange};

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
            ///Date and time, it is to separate same queries, which is plausible thing to happen
            datetime: &str,
            ///Pub key
            pub_key: &PublicKey
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

impl AuditBlocks {
    pub fn retrieve_changes_from_neo4j(&self, fork: &Fork) -> Vec<BlockChangesResponse> {
        let schema: Schema<&Fork> = Schema::new(fork);
        let core_schema: CoreSchema<&Fork> = CoreSchema::new(&fork);
        let all_blocks_by_height = core_schema.block_hashes_by_height();
        let all_blocks = core_schema.blocks();

        let mut returnVector: Vec<BlockChangesResponse> = Vec::new();

        let neo4j_rpc = get_neo4j_rpc_client();

        let last_block_option = schema.get_last_confirmed_block();
        let mut last_block_index = 0;
        match last_block_option {
            Some(block_hash) => {
                let block_option = all_blocks.get(&block_hash);
                match block_option {
                    Some(block) => {
                        last_block_index = block.height().0 + 1;
                    }
                    None => {
                        println!("ERROR: Should not be here 001"); //shouldn't get here
                    }
                }
            }
            None => {}
        }
        for x in last_block_index..all_blocks_by_height.len() {
            let block_hash_option = all_blocks_by_height.get(x);
            match block_hash_option {
                Some(block_hash) => {
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
                        continue;
                    }

                    match neo4j_rpc.retrieve_block_changes(block_hash) {
                        ChangeResponse(changes) => returnVector.push(changes),
                        DBError(e) => println!("{:?}", e.msg()), //TODO error handling
                        _ => {}
                    }
                }
                None => {
                    println!("ERROR: Should not be here 002");
                } //shouldn't get here
            }
        }
        returnVector
    }

    pub fn add_changes_to_exonum(
        &self,
        fork: &mut Fork,
        changes_per_block: Vec<BlockChangesResponse>,
        current_transaction: Hash,
    ) {
        let mut schema: Schema<&mut Fork> = Schema::new(fork);
        for block_changes in changes_per_block {
            match Hash::from_hex(block_changes.get_block_id()) {
                Ok(block_hash) => {
                    schema.add_audited_block(&current_transaction, block_hash);
                }
                _ => {
                    println!("ERROR: Should not be here 003");
                } //Should not get here
            }
            for transaction_changes in block_changes.get_transactions() {
                match Hash::from_hex(transaction_changes.get_transaction_id()) {
                    Ok(transaction_hash) => match transaction_changes.get_result() {
                        Status::SUCCESS => {
                            let changes = transaction_changes.get_modifications();
                            let change_vec: Vec<NodeChange> = generate_database_changes_from_proto(
                                changes,
                                &mut schema,
                                transaction_changes.get_transaction_id(),
                            );
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

                            let error_msg = if failed_query.get_query().is_empty() {
                                format!("{}", error.get_message())
                            } else {
                                format!(
                                    "{}\n|||Happened in query: {}\n|||Error: {}",
                                    error.get_message(),
                                    failed_query.get_query(),
                                    failed_query.get_error()
                                )
                            };
                            schema.update_neo4j_transaction(
                                &transaction_hash,
                                error_msg.as_str(),
                                "ERROR",
                            );
                        }
                    },
                    _ => {}
                }
            }
        }
    }

    pub fn update_last_block(&self, fork: &mut Fork) {
        let last_block = { CoreSchema::new(&fork).block_hashes_by_height().last() };

        let mut schema: Schema<&mut Fork> = Schema::new(fork);
        match last_block {
            Some(hash) => schema.set_last_confirmed_block(hash),
            None => {}
        }
    }
}

impl Transaction for AuditBlocks {
    fn verify(&self) -> bool {
        true
    }

    fn execute(&self, fork: &mut Fork) -> ExecutionResult {
        let hash = self.hash();
        let changes = self.retrieve_changes_from_neo4j(fork);
        self.add_changes_to_exonum(fork, changes, hash);
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
        let pub_key = println!("Adding transaction: {}", self.queries());

        let q = Neo4jTransaction::new(self.queries(), "", "PENDING", self.pub_key());

        schema.add_neo4j_transaction(q, &hash);
        Ok(())
    }
}
