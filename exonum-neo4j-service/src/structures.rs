#![allow(warnings)]

extern crate protobuf;

use exonum::crypto::{CryptoHash, Hash, hash};
use exonum::storage::StorageValue;
use std::borrow::Cow;
use std::fmt;
use structures::NodeChange::AN;
use structures::NodeChange::AR;
use gRPCProtocol::TransactionRequest;



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
    struct AddRelation {
        rel_uuid: &str,
        from_uuid: &str,
        to_uuid: &str
    }
}

#[derive(Clone, Debug)]
pub enum NodeChange {
    AN(AddNode),
    AR(AddRelation)
}

impl StorageValue for NodeChange {
    fn into_bytes(self) -> Vec<u8> {
        match self {
            AN(an) => {
                let mut bytes = an.raw;
                bytes.push(1);
                bytes
            },
            AR(ar) => {
                let mut bytes = ar.raw;
                bytes.push(2);
                bytes
            }
        }
    }

    fn from_bytes(v: ::std::borrow::Cow<[u8]>) -> Self {
        let mut data =  v.into_owned();
        let i = data.pop();

        let nc : NodeChange = match i {
            Some(1) => AN(AddNode::from_bytes(Cow::Borrowed(&data))),
            _ => AR(AddRelation::from_bytes(Cow::Borrowed(&data)))
        };
        nc
    }
}

impl CryptoHash for NodeChange {
    fn hash(&self) -> Hash {
        match self {
            AN(an) => {
                hash(an.raw.as_ref())
            },
            AR(ar) => {
                hash(ar.raw.as_ref())
            }
        }
    }
}

impl fmt::Display for NodeChange {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match self {
            AN(an) => write!(f, "Node with UUID {} created", an.node_uuid()),
            AR(ar) => write!(f, "Relationship with UUID {} added. Starting from {}, and going to {}", ar.rel_uuid(), ar.from_uuid(), ar.to_uuid()),
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

impl Queries {


    pub fn execute(&self) -> Vec<NodeChange> {
        let c1 = NodeChange::AN(AddNode::new(
            "u1"
        ));
        let c2 = NodeChange::AR(AddRelation::new(
            "r1",
            "u1",
            "u2"
        ));
        vec![c1, c2]
    }
}

impl NodeChange {
    pub fn get_uuis(&self) -> Vec<&str>{
        match self {
            AN(an) => vec![an.node_uuid()],
            AR(ar) => vec![ar.from_uuid(), ar.to_uuid()],
        }
    }
}

