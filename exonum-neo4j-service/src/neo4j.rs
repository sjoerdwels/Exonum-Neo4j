
//! Module of the rust-protobuf generated files.
#![allow(bare_trait_objects)]
#![allow(renamed_and_removed_lints)]

pub use self::transaction_manager::*;
pub use self::transaction_manager_grpc::*;

// Include generated protobuf files
include!(concat!(env!("OUT_DIR"), "/protobuf_mod.rs"));

// todo: fix imports
use std::sync::Arc;
use std::vec::Vec;
use std::string::String;
use grpc::{Client, ClientStub, RequestOptions};
use exonum::{crypto::Hash, storage::{Fork, Snapshot}, blockchain::{Schema as CoreSchema, Block}};

use structures::*;
use structures::NodeChange::{AN, RN, ANP, RNP, AL, RL, AR, ARP, RRP};
use self::ExecuteResponse::*;

use schema::Schema;

pub struct Neo4jConfig {
    pub address : String,
    pub port : u16
}

pub struct Neo4jRpc {
    transaction_manager : TransactionManagerClient
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

    pub fn execute_block(&self, block: Block, block_hash: &str, core_schema : CoreSchema<&Snapshot>, schema : Schema<&Snapshot>) -> ExecuteResponse {

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
pub fn generate_database_changes_from_proto(modifs : &DatabaseModifications, schema : &mut Schema<&mut Fork>) -> Vec<NodeChange>{
    let mut changes : Vec<NodeChange> = Vec::new();
    for new_node in modifs.get_created_nodes() {
        let new_change = AddNode::new(new_node.get_node_UUID());
        changes.push(AN(new_change));
    }
    for removed_node in modifs.get_deleted_nodes() {
        let new_change = RemoveNode::new(removed_node.get_node_UUID());
        changes.push(RN(new_change));
    }
    for new_label in modifs.get_assigned_labels() {
        let new_change = AddLabel::new(new_label.get_node_UUID(), new_label.get_name());
        changes.push(AL(new_change));
    }
    for remove_label in modifs.get_removed_labels() {
        let new_change = RemoveLabel::new(remove_label.get_node_UUID(), remove_label.get_name());
        changes.push(RL(new_change));
    }
    for new_node_property in modifs.get_assigned_node_properties() {
        let new_change = AddNodeProperty::new(new_node_property.get_node_UUID(), new_node_property.get_key(), new_node_property.get_value());
        changes.push(ANP(new_change));
    }
    for remove_node_property in modifs.get_removed_node_properties() {
        let new_change = RemoveNodeProperty::new(remove_node_property.get_node_UUID(), remove_node_property.get_key());
        changes.push(RNP(new_change));
    }
    for new_relation in modifs.get_created_relationships() {
        let new_change = AddRelation::new(new_relation.get_relationship_UUID(), new_relation.get_field_type(), new_relation.get_start_node_UUID(), new_relation.get_end_node_UUID());
        changes.push(AR(new_change));
        let r : Relation = Relation::new(new_relation.get_start_node_UUID(), new_relation.get_end_node_UUID());
        schema.add_relation(r, new_relation.get_relationship_UUID());
    }

    for new_relation_property in modifs.get_assigned_relationship_properties() {
        match schema.relation(new_relation_property.get_relationship_UUID()) {
            Some(relation) => {
                let new_change = AddRelationProperty::new(new_relation_property.get_relationship_UUID(),
                     new_relation_property.get_key(), new_relation_property.get_value(), relation.start_node_uuid(), relation.end_node_uuid());
                changes.push(ARP(new_change));
            },
            _ => {} //TODO what if relationship is not found?
        }
    }

    for remove_relation_property in modifs.get_removed_relation_properties() {
        match schema.relation(remove_relation_property.get_relationship_UUID()) {
            Some(relation) => {
                let new_change = RemoveRelationProperty::new(remove_relation_property.get_relationship_UUID(), "",
                        relation.start_node_uuid(), relation.end_node_uuid());
                changes.push(RRP(new_change));
            },
            _ => {} //TODO what if relationship is not found?
        }
    }
    changes
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