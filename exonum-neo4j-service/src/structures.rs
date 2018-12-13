#![allow(warnings)]

extern crate protobuf;

use exonum::crypto::{CryptoHash, Hash, hash};
use exonum::storage::StorageValue;
use std::borrow::Cow;
use std::fmt;
use structures::NodeChange::{AN, RN, ANP, RNP, AL, RL, AR};
use gRPCProtocol::{TransactionRequest, DatabaseModifications, Status};
use gRPCProtocol_grpc::{getClient, TransactionManager};
use grpc::RequestOptions;





encoding_struct! {
    ///Our test variable, which we are going to change.
    struct Queries {
        queries: &str,
        transaction_hash: &Hash
    }
}

encoding_struct! {
    struct AddNode {
        node_uuid: &str
    }
}

encoding_struct! {
    struct RemoveNode {
        node_uuid: &str
    }
}

encoding_struct! {
    struct AddRelation {
        rel_uuid: &str,
        field_type: &str,
        from_uuid: &str,
        to_uuid: &str
    }
}

encoding_struct! {
    struct AddLabel {
        node_uuid: &str,
        label_name: &str,
    }
}

encoding_struct! {
    struct RemoveLabel {
        node_uuid: &str,
        label_name: &str,
    }
}

encoding_struct! {
    struct AddNodeProperty {
        node_uuid: &str,
        key: &str,
        value: &str,
    }
}

encoding_struct! {
    struct RemoveNodeProperty {
        node_uuid: &str,
        key: &str,
    }
}


#[derive(Clone, Debug)]
pub enum NodeChange {
    AN(AddNode),
    RN(RemoveNode),
    AR(AddRelation),
    AL(AddLabel),
    RL(RemoveLabel),
    ANP(AddNodeProperty),
    RNP(RemoveNodeProperty),
}
encoding_struct! {
    struct ErrorMsg {
        msg: &str,
    }
}

#[derive(Clone, Debug)]
pub enum ExecuteResponse {
    Changes(Vec<NodeChange>),
    Error(ErrorMsg),
}

impl StorageValue for NodeChange {
    fn into_bytes(self) -> Vec<u8> {
        match self {
            AN(x) => {
                let mut bytes = x.raw;
                bytes.push(1);
                bytes
            },
            RN(x) => {
                let mut bytes = x.raw;
                bytes.push(2);
                bytes
            },
            ANP(x) => {
                let mut bytes = x.raw;
                bytes.push(3);
                bytes
            },
            RNP(x) => {
                let mut bytes = x.raw;
                bytes.push(4);
                bytes
            },
            AL(x) => {
                let mut bytes = x.raw;
                bytes.push(5);
                bytes
            },
            RL(x) => {
                let mut bytes = x.raw;
                bytes.push(6);
                bytes
            },
            AR(x) => {
                let mut bytes = x.raw;
                bytes.push(7);
                bytes
            }
        }
    }

    fn from_bytes(v: ::std::borrow::Cow<[u8]>) -> Self {
        let mut data =  v.into_owned();
        let i = data.pop();

        let nc : NodeChange = match i {
            Some(1) => AN(AddNode::from_bytes(Cow::Borrowed(&data))),
            Some(2) => RN(RemoveNode::from_bytes(Cow::Borrowed(&data))),
            Some(3) => ANP(AddNodeProperty::from_bytes(Cow::Borrowed(&data))),
            Some(4) => RNP(RemoveNodeProperty::from_bytes(Cow::Borrowed(&data))),
            Some(5) => AL(AddLabel::from_bytes(Cow::Borrowed(&data))),
            Some(6) => RL(RemoveLabel::from_bytes(Cow::Borrowed(&data))),
            _ => AR(AddRelation::from_bytes(Cow::Borrowed(&data)))
        };
        nc
    }
}

impl CryptoHash for NodeChange {
    fn hash(&self) -> Hash {
        match self {
            AN(x) => {
                hash(x.raw.as_ref())
            },
            RN(x) => {
                hash(x.raw.as_ref())
            },
            ANP(x) => {
                hash(x.raw.as_ref())
            },
            RNP(x) => {
                hash(x.raw.as_ref())
            },
            AL(x) => {
                hash(x.raw.as_ref())
            },
            RL(x) => {
                hash(x.raw.as_ref())
            },

            AR(x) => {
                hash(x.raw.as_ref())
            }
        }
    }
}

impl fmt::Display for NodeChange {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match self {
            AN(x) => write!(f, "Node with UUID {} created", x.node_uuid()),
            RN(x) => write!(f, "Node with UUID {} deleted", x.node_uuid()),
            AL(x) => write!(f, "Label, {}, added to node", x.label_name()),
            RL(x) => write!(f, "Label, {}, removed from node", x.label_name()),
            ANP(x) => write!(f, "Added new property, key {}, value {}", x.key(), x.value()),
            RNP(x) => write!(f, "Removed property, key {}", x.key()),
            AR(x) => write!(f, "Relationship with UUID {} added. Starting from {}, and going to {}", x.rel_uuid(), x.from_uuid(), x.to_uuid()),
        }
    }
}

pub fn getProtoTransactionRequest(queries: &str, prefix: &str) -> TransactionRequest {
    let split = queries.split(";");
    let vec: Vec<::std::string::String> = split.map(|s| s.to_string()).collect();
    let protoVec = protobuf::RepeatedField::from_vec(vec);
    let mut req = TransactionRequest::new();
    req.set_UUID_prefix(prefix.to_string());
    req.set_queries(protoVec);
    req

}

pub fn getNodeChangeVector(modifs : &DatabaseModifications) -> Vec<NodeChange>{
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
    }
    changes
}

impl Queries {


    pub fn execute(&self) -> ExecuteResponse {
        let req = getProtoTransactionRequest(self.queries(), self.transaction_hash().to_hex().as_str());
        println!("prefix gonna be {}", req.get_UUID_prefix());
        //TODO implement getting neo4J server info from conf somehow.
        let client = getClient(9994);
        let resp = client.execute(RequestOptions::new(), req);
        let answer = resp.wait();
        match answer {
            Ok(x) => {
                match x.1.get_result() {
                    Status::SUCCESS => {
                        let changes = x.1.get_modifications();
                        let rVec : Vec<NodeChange> =  getNodeChangeVector(changes);
                        return ExecuteResponse::Changes(rVec);
                    }
                    Status::FAILURE => {}
                }
            },
            _ => {} //TODO must raise proper error to client! Understand Error of result.
        }
        ExecuteResponse::Changes(vec![])
    }
}

impl NodeChange {
    pub fn get_uuis(&self) -> Vec<&str>{
        match self {
            AN(x) => vec![x.node_uuid()],
            RN(x) => vec![x.node_uuid()],
            ANP(x) => vec![x.node_uuid()],
            RNP(x) => vec![x.node_uuid()],
            AL(x) => vec![x.node_uuid()],
            RL(x) => vec![x.node_uuid()],
            AR(x) => vec![x.from_uuid(), x.to_uuid()],
        }
    }
}

