
//! Module of the rust-protobuf generated files.
#![allow(bare_trait_objects)]
#![allow(renamed_and_removed_lints)]

pub use self::transaction_manager::*;
pub use self::transaction_manager_grpc::*;

// Include generated protobuf files
include!(concat!(env!("OUT_DIR"), "/protobuf_mod.rs"));

// todo: fix imports
use std::sync::Arc;
use grpc::{Client, ClientStub};

use structures::*;
use structures::NodeChange::{AN, RN, ANP, RNP, AL, RL, AR, ARP, RRP};

use schema::Schema;

pub struct Neo4jConfig {
    address : String,
    port : u16
}

pub struct Neo4jRpc {
    config: Neo4jConfig,
    transaction_manager : TransactionManagerClient
}

impl Neo4jRpc {

    /// Creates a new Neo4j RPC handler
    pub fn new(config : Neo4jConfig) -> Self {
        let client_conf = Default::default();
        let grpc_client = Arc::new(Client::new_plain(&config.address, config.port, client_conf).unwrap());
        let transaction_manager =  TransactionManagerClient::with_client(grpc_client);
        Neo4jRpc {
            config,
            transaction_manager
        }
    }

     pub fn execute(&self, Vec<::std::string::String> queries, String prefix ) -> ExecuteResponse {

        // Create request message
        let mut request = TransactionRequest::new();
        let queries = protobuf::RepeatedField::from_vec(queries);
        request.set_queries(queries);
        request.set_UUID_prefix(prefix);

        println!("prefix gonna be {}", request.get_UUID_prefix());

        // Execute request
        self.transaction_manager.execute(RequestOptions::new(), request);

        let result = request.wait();

        // Handle result
        match result {
            Ok(x) => {
                match x.1.get_result() {
                    Status::SUCCESS => {
                        let changes = x.1.get_modifications();
                        let rVec : Vec<NodeChange> =  getNodeChangeVector(changes, schema);
                        return ExecuteResponse::Changes(rVec);
                    }
                    Status::FAILURE => {
                        let error = x.1.get_error();
                        let failed_query = error.get_failed_query();
                        let error_msg = format!("{}\nHappened in query: {}\n{}", error.get_message(), failed_query.get_query(), failed_query.get_error());
                        return ExecuteResponse::Error(ErrorMsg::new(error_msg.as_str()));
                    }
                }
            },
            Err(e) => {
                return ExecuteResponse::Error(ErrorMsg::new(format!("{:?}", e).as_str()))
            } //TODO must raise proper error to client! Understand Error of result.
        }

        ExecuteResponse::Changes(vec![])
    }

    ///  Get DB changes list from proto
    fn generate_database_changes_from_proto(&self, modifs : &DatabaseModifications, schema : &mut Schema<&mut Fork>) -> Vec<NodeChange>{
        let mut changes : Vec<NodeChange> = Vec::new();
        for newNode in modifs.get_created_nodes() {
            let newChange = AddNode::new(newNode.get_node_UUID());
            changes.push(AN(newChange));
        }
        for removedNode in modifs.get_deleted_nodes() {
            let newChange = RemoveNode::new(removedNode.get_node_UUID());
            changes.push(RN(newChange));
        }
        for newLabel in modifs.get_assigned_labels() {
            let newChange = AddLabel::new(newLabel.get_node_UUID(), newLabel.get_name());
            changes.push(AL(newChange));
        }
        for removeLabel in modifs.get_removed_labels() {
            let newChange = RemoveLabel::new(removeLabel.get_node_UUID(), removeLabel.get_name());
            changes.push(RL(newChange));
        }
        for newNodeProperty in modifs.get_assigned_node_properties() {
            let newChange = AddNodeProperty::new(newNodeProperty.get_node_UUID(), newNodeProperty.get_key(), newNodeProperty.get_value());
            changes.push(ANP(newChange));
        }
        for removeNodeProperty in modifs.get_removed_node_properties() {
            let newChange = RemoveNodeProperty::new(removeNodeProperty.get_node_UUID(), removeNodeProperty.get_key());
            changes.push(RNP(newChange));
        }
        for newRelation in modifs.get_created_relationships() {
            let newChange = AddRelation::new(newRelation.get_relationship_UUID(), newRelation.get_field_type(), newRelation.get_start_node_UUID(), newRelation.get_end_node_UUID());
            changes.push(AR(newChange));
            let r : Relation = Relation::new(newRelation.get_start_node_UUID(), newRelation.get_end_node_UUID());
            schema.add_relation(r, newRelation.get_relationship_UUID());
        }

        for addRelationProperty in modifs.get_assigned_relationship_properties() {
            match schema.relation(addRelationProperty.get_relationship_UUID()) {
                Some(relation) => {
                    let newChange = AddRelationProperty::new(addRelationProperty.get_relationship_UUID(),
                         addRelationProperty.get_key(), addRelationProperty.get_value(), relation.start_node_uuid(), relation.end_node_uuid());
                    changes.push(ARP(newChange));
                },
                _ => {} //TODO what if relationship is not found?
            }
        }

        for removeRelationProperty in modifs.get_removed_relation_properties() {
            match schema.relation(removeRelationProperty.get_relationship_UUID()) {
                Some(relation) => {
                    let newChange = RemoveRelationProperty::new(removeRelationProperty.get_relationship_UUID(), "",
                            relation.start_node_uuid(), relation.end_node_uuid());
                    changes.push(RRP(newChange));
                },
                _ => {} //TODO what if relationship is not found?
            }
        }
        changes
    }
}
