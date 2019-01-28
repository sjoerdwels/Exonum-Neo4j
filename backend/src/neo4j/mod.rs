///Module for communicating with neo4j.

pub mod proto;

pub use neo4j::proto::transaction_manager::*;
pub use neo4j::proto::transaction_manager_grpc::*;

// todo: fix imports
use std::sync::Arc;
use std::vec::Vec;
use std::fmt;
use std::string::String;
use grpc::{Client, ClientStub, RequestOptions};
use exonum::{crypto::Hash, storage::{Fork, Snapshot}, blockchain::{Schema as CoreSchema, Block}};
use util::parse_port;

use structures::*;
use structures::NodeChange::{AN, RN, ANP, RNP, AL, RL, AR, ARP, RRP};
use self::ExecuteResponse::*;

use schema::Schema;


///Neo4j Config structure.
#[derive(Debug)]
pub struct Neo4jConfig {
    ///neo4j address
    pub address : String,
    ///neo4j port
    pub port : u16
}

///Neo4j RPC struct, has the transaction_manager which is able to call gRPC to Neo4j, and implements wrapper functionality
pub struct Neo4jRpc {
    ///generated transaction manager which is able to make gRPC calls to Neo4j
    transaction_manager : TransactionManagerClient
}

impl fmt::Debug for Neo4jRpc {
    fn fmt(&self, f: &mut fmt::Formatter) ->fmt::Result {
        f.debug_struct("Neo4j RPC").finish()
    }
}


impl Neo4jRpc {

    /// Creates a new Neo4j RPC handler
    pub fn new(config : Neo4jConfig) -> Self {
        let client_conf = Default::default();
        let grpc_client = Arc::new(Client::new_plain(&config.address, config.port, client_conf).unwrap());
        let transaction_manager =  TransactionManagerClient::with_client(grpc_client);
        Neo4jRpc {
            transaction_manager
        }
    }

    ///Function that asks neo4j to execute the whole block. It retrieves block transactions from Schema. If there are no transactions returns NoCommit(())
    pub fn execute_block(&self, block: &Block, block_hash: &str, core_schema : &CoreSchema<&dyn Snapshot>, schema : &Schema<&dyn Snapshot>) -> ExecuteResponse {

        let transactions = core_schema.block_transactions(block.height());

        let mut request = BlockExecuteRequest::new();
        request.set_block_id(block_hash.to_string());
        let mut trans_vector : Vec<TransactionRequest> = Vec::new();
        for trans_hash in transactions.iter() {
            let potential_trans = schema.neo4j_transaction(&trans_hash);
            match potential_trans {
                Some(neo4j_transaction) => {
                    let queries = neo4j_transaction.queries().split(";");
                    let query_vector: Vec<::std::string::String> = queries.map(|s| s.to_string()).collect();
                    let proto_vec = protobuf::RepeatedField::from_vec(query_vector);
                    let mut trans_req = TransactionRequest::new();
                    trans_req.set_transaction_id(trans_hash.to_hex().as_str().to_string());
                    trans_req.set_queries(proto_vec);
                    trans_vector.push(trans_req);
                },
                None => {}
            }

        }
        if trans_vector.len() > 0 {
            request.set_transactions(protobuf::RepeatedField::from_vec(trans_vector));
            let resp = self.transaction_manager.execute_block(RequestOptions::new(), request);

            let result = resp.wait();
            match result {
                Ok(_) => {
                    OkExe(())
                },
                Err(e) => {
                    ExecuteResponse::Error(ErrorMsg::new(format!("{:?}", e).as_str()))
                }
            }
        } else {
            NoCommits(())
        }
    }

    ///Ask Neo4j to remove audited changes. These are changes that were audited during the given block.
    /// Meaning there had to have been an AuditBlocks transaction, which saved changes for certain blocks.
    /// These blocks are stored in schema.audited_blocks
    pub fn remove_audited_changes(&self, block: Block, core_schema : CoreSchema<&dyn Snapshot>, schema : Schema<&dyn Snapshot>){
        let transactions = core_schema.block_transactions(block.height());
        for trans_hash in transactions.iter() {
            let audited_blocks = schema.audited_blocks(&trans_hash);
            for block in audited_blocks.iter() {
                let mut request = DeleteBlockRequest::new();
                request.set_block_id(block.to_hex().as_str().to_string());
                let resp = self.transaction_manager.delete_block_changes(RequestOptions::new(), request);
                match resp.wait() {
                    Ok(x) => {
                        match x.1.get_success() {
                            true => println!("Succesfully deleted changes in neo4j for {}",block.to_hex().as_str() ),
                            false => println!("Failed to deleted changes in neo4j for {}",block.to_hex().as_str() ),
                        }
                    },
                    Err(e) => {
                        println!("{:?}", e)
                    }
                }
            }
        }
    }

    ///Retrieves block changes, given block_hash.
    pub fn retrieve_block_changes(&self, block_hash: Hash) -> ExecuteResponse {
        let mut request = BlockChangesRequest::new();
        request.set_block_id(block_hash.to_hex().as_str().to_string());
        let resp = self.transaction_manager.retrieve_block_changes(RequestOptions::new(), request);

        let result = resp.wait();
        match result {
            Ok(x) => {
                ChangeResponse(x.1)
            },
            Err(e) => {
                ExecuteResponse::Error(ErrorMsg::new(format!("{:?}", e).as_str()))
            }
        }
    }
}

///  Get DB changes list from proto
pub fn generate_database_changes_from_proto(modifs : &DatabaseModifications, schema : &mut Schema<&mut Fork>, transaction_id: &str) -> Vec<NodeChange>{
    let mut changes : Vec<NodeChange> = Vec::new();
    for new_node in modifs.get_created_nodes() {
        let new_change = AddNode::new(new_node.get_node_UUID(), transaction_id);
        changes.push(AN(new_change));
    }
    for removed_node in modifs.get_deleted_nodes() {
        let new_change = RemoveNode::new(removed_node.get_node_UUID(), transaction_id);
        changes.push(RN(new_change));
    }
    for new_label in modifs.get_assigned_labels() {
        let new_change = AddLabel::new(new_label.get_node_UUID(), new_label.get_name(), transaction_id);
        changes.push(AL(new_change));
    }
    for remove_label in modifs.get_removed_labels() {
        let new_change = RemoveLabel::new(remove_label.get_node_UUID(), remove_label.get_name(), transaction_id);
        changes.push(RL(new_change));
    }
    for new_node_property in modifs.get_assigned_node_properties() {
        let new_change = AddNodeProperty::new(new_node_property.get_node_UUID(), new_node_property.get_key(), new_node_property.get_value(), transaction_id);
        changes.push(ANP(new_change));
    }
    for remove_node_property in modifs.get_removed_node_properties() {
        let new_change = RemoveNodeProperty::new(remove_node_property.get_node_UUID(), remove_node_property.get_key(), transaction_id);
        changes.push(RNP(new_change));
    }
    for new_relation in modifs.get_created_relationships() {
        let new_change = AddRelation::new(new_relation.get_relationship_UUID(), new_relation.get_field_type(), new_relation.get_start_node_UUID(), new_relation.get_end_node_UUID(), transaction_id);
        changes.push(AR(new_change));
        let r : Relation = Relation::new(new_relation.get_start_node_UUID(), new_relation.get_end_node_UUID());
        schema.add_relation(r, new_relation.get_relationship_UUID());
    }

    for new_relation_property in modifs.get_assigned_relationship_properties() {
        match schema.relation(new_relation_property.get_relationship_UUID()) {
            Some(relation) => {
                let new_change = AddRelationProperty::new(new_relation_property.get_relationship_UUID(),
                     new_relation_property.get_key(), new_relation_property.get_value(), relation.start_node_uuid(), relation.end_node_uuid(), transaction_id);
                changes.push(ARP(new_change));
            },
            _ => {println!("ERROR: Should not be here 004");}
        }
    }

    for remove_relation_property in modifs.get_removed_relation_properties() {
        match schema.relation(remove_relation_property.get_relationship_UUID()) {
            Some(relation) => {
                let new_change = RemoveRelationProperty::new(remove_relation_property.get_relationship_UUID(), remove_relation_property.get_key(),
                        relation.start_node_uuid(), relation.end_node_uuid(), transaction_id);
                changes.push(RRP(new_change));
            },
            _ => {println!("ERROR: Should not be here 005");}
        }
    }
    changes
}

///Gets an neo4j rpc client, with the port defined in neo4j.toml
pub fn get_neo4j_rpc_client() -> Neo4jRpc {
    let mut port = 9994;
    match parse_port() {
        Ok(p) => port = p,
        _ => {}
    }
    let neo4j_config = Neo4jConfig{
        address : String::from("127.0.0.1"),
        port : port
    };
    let neo4j_rpc = Neo4jRpc::new(neo4j_config);
    neo4j_rpc
}

///Response we get from communicating with neo4j
#[derive(Clone, Debug)]
pub enum ExecuteResponse {
    ///List of changes
    OkExe(()),
    ///error when something went wrong
    Error(ErrorMsg),
    ///RetrieveChangesResponse
    ChangeResponse(BlockChangesResponse),
    ///Block had no commit transactions, propably only audit.
    NoCommits(()),
}