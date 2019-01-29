//!Defines all basic structures and implements them when necessary

#![allow(warnings)]

extern crate protobuf;

use exonum::storage::StorageValue;
use exonum::{
    crypto::{hash, CryptoHash, Hash, PublicKey},
    storage::Fork,
};
use grpc::RequestOptions;
use schema::Schema;
use std::borrow::Cow;
use std::fmt;
use structures::NodeChange::{AL, AN, ANP, AR, ARP, RL, RN, RNP, RRP};
use util;

///add node
encoding_struct! {
    ///add node
    struct AddNode {
        ///node uuid, made of transaction hash for prefix and index.
        node_uuid: &str,
        ///hash value for the the transaction it is part of
        transaction_id: &str,
    }
}

///remove node
encoding_struct! {
    ///remove node
    struct RemoveNode {
        ///node uuid
        node_uuid: &str,
        ///hash value for the the transaction it is part of
        transaction_id: &str,
    }
}

///add relation
encoding_struct! {
    ///add relation
    struct AddRelation {
        ///relation uuid, needed since we add to our own database
        rel_uuid: &str,
        ///relationship type
        field_type: &str,
        ///from node uuid
        from_uuid: &str,
        ///to node uuid
        to_uuid: &str,
        ///hash value for the the transaction it is part of
        transaction_id: &str,
    }
}

///add label
encoding_struct! {
    ///add label
    struct AddLabel {
        ///node uuid
        node_uuid: &str,
        ///label name
        label_name: &str,
        ///hash value for the the transaction it is part of
        transaction_id: &str,
    }
}

///remove label
encoding_struct! {
    ///Remove label
    struct RemoveLabel {
        ///Node uuid
        node_uuid: &str,
        ///label name
        label_name: &str,
        ///hash value for the the transaction it is part of
        transaction_id: &str,
    }
}

///add new property
encoding_struct! {
    ///Add new property
    struct AddNodeProperty {
        ///Node uuid
        node_uuid: &str,
        ///property key
        key: &str,
        ///property new value
        value: &str,
        ///hash value for the the transaction it is part of
        transaction_id: &str,
    }
}

///remove node property
encoding_struct! {
    ///remove node property
    struct RemoveNodeProperty {
        ///Node uuid
        node_uuid: &str,
        ///property key
        key: &str,
        ///hash value for the the transaction it is part of
        transaction_id: &str,
    }
}

///Add relation property
encoding_struct! {
    ///Add relation property
    struct AddRelationProperty {
        ///relation's uuid
        relation_uuid: &str,
        ///property key
        key: &str,
        ///property new value
        value: &str,
        ///from node uuid
        from_uuid: &str,
        ///to node uuid
        to_uuid: &str,
        ///hash value for the the transaction it is part of
        transaction_id: &str,
    }
}

///Remove relation property
encoding_struct! {
    ///Remove relation property
    struct RemoveRelationProperty {
        ///relation's uuid
        relation_uuid: &str,
        ///property key
        key: &str,
        ///from node uuid
        from_uuid: &str,
        ///to node uuid
        to_uuid: &str,
        ///hash value for the the transaction it is part of
        transaction_id: &str,
    }
}

///All possible node changes
#[derive(Clone, Debug)]
pub enum NodeChange {
    ///Add new node
    AN(AddNode),
    ///Remove existing node
    RN(RemoveNode),
    ///Add new relation
    AR(AddRelation),
    ///Add new label
    AL(AddLabel),
    ///Remove existing label
    RL(RemoveLabel),
    ///Add new node property, Could be part of modification.
    ANP(AddNodeProperty),
    ///Delete existing node property, Could be part of modification.
    RNP(RemoveNodeProperty),
    ///Add new relation property. Could be part of modification.
    ARP(AddRelationProperty),
    ///Remove existing relation property. Could be part of modification.
    RRP(RemoveRelationProperty),
}

impl StorageValue for NodeChange {
    fn into_bytes(self) -> Vec<u8> {
        match self {
            AN(x) => {
                let mut bytes = x.raw;
                bytes.push(1);
                bytes
            }
            RN(x) => {
                let mut bytes = x.raw;
                bytes.push(2);
                bytes
            }
            ANP(x) => {
                let mut bytes = x.raw;
                bytes.push(3);
                bytes
            }
            RNP(x) => {
                let mut bytes = x.raw;
                bytes.push(4);
                bytes
            }
            AL(x) => {
                let mut bytes = x.raw;
                bytes.push(5);
                bytes
            }
            RL(x) => {
                let mut bytes = x.raw;
                bytes.push(6);
                bytes
            }
            AR(x) => {
                let mut bytes = x.raw;
                bytes.push(7);
                bytes
            }
            ARP(x) => {
                let mut bytes = x.raw;
                bytes.push(8);
                bytes
            }
            RRP(x) => {
                let mut bytes = x.raw;
                bytes.push(9);
                bytes
            }
        }
    }

    fn from_bytes(v: ::std::borrow::Cow<[u8]>) -> Self {
        let mut data = v.into_owned();
        let i = data.pop();

        let nc: NodeChange = match i {
            Some(1) => AN(AddNode::from_bytes(Cow::Borrowed(&data))),
            Some(2) => RN(RemoveNode::from_bytes(Cow::Borrowed(&data))),
            Some(3) => ANP(AddNodeProperty::from_bytes(Cow::Borrowed(&data))),
            Some(4) => RNP(RemoveNodeProperty::from_bytes(Cow::Borrowed(&data))),
            Some(5) => AL(AddLabel::from_bytes(Cow::Borrowed(&data))),
            Some(6) => RL(RemoveLabel::from_bytes(Cow::Borrowed(&data))),
            Some(8) => ARP(AddRelationProperty::from_bytes(Cow::Borrowed(&data))),
            Some(9) => RRP(RemoveRelationProperty::from_bytes(Cow::Borrowed(&data))),
            _ => AR(AddRelation::from_bytes(Cow::Borrowed(&data))),
        };
        nc
    }
}

impl CryptoHash for NodeChange {
    fn hash(&self) -> Hash {
        match self {
            AN(x) => hash(x.raw.as_ref()),
            RN(x) => hash(x.raw.as_ref()),
            ANP(x) => hash(x.raw.as_ref()),
            RNP(x) => hash(x.raw.as_ref()),
            AL(x) => hash(x.raw.as_ref()),
            RL(x) => hash(x.raw.as_ref()),

            AR(x) => hash(x.raw.as_ref()),
            ARP(x) => hash(x.raw.as_ref()),
            RRP(x) => hash(x.raw.as_ref()),
        }
    }
}

impl fmt::Display for NodeChange {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match self {
            AN(x) => write!(f, "Node with UUID <b>{}</b> created", x.node_uuid()),
            RN(x) => write!(f, "Node with UUID <b>{}</b> deleted", x.node_uuid()),
            AL(x) => write!(f, "Label, <b>{}</b>, added to node", x.label_name()),
            RL(x) => write!(f, "Label, <b>{}</b>, removed from node", x.label_name()),
            ANP(x) => write!(f, "Added new property, key <b>{}</b>, value <b>{}</b>", x.key(), x.value()),
            RNP(x) => write!(f, "Removed property, key <b>{}</b>", x.key()),
            AR(x) => write!(f, "Relationship of type <b>{}</b> with UUID <b>{}</b> added. Starting from <b>{}</b>, and going to <b>{}</b>", x.field_type(), x.rel_uuid(), x.from_uuid(), x.to_uuid()),
            ARP(x) => write!(f, "Property, <b>{}</b>, with value <b>{}</b>, added to relation with uuid <b>{}</b>", x.key(), x.value(), x.relation_uuid()),
            RRP(x) => write!(f, "Property, <b>{}</b>, removed to relation with uuid <b>{}</b>", x.key(), x.relation_uuid()),
        }
    }
}

impl NodeChange {
    ///This defines the logic of which nodes we add specific changes. Some changes, related to relations we have to add to both end points.
    pub fn get_uuis(&self) -> Vec<&str> {
        match self {
            AN(x) => vec![x.node_uuid()],
            RN(x) => vec![x.node_uuid()],
            ANP(x) => vec![x.node_uuid()],
            RNP(x) => vec![x.node_uuid()],
            AL(x) => vec![x.node_uuid()],
            RL(x) => vec![x.node_uuid()],
            AR(x) => vec![x.from_uuid(), x.to_uuid()],
            ARP(x) => vec![x.from_uuid(), x.to_uuid()],
            RRP(x) => vec![x.from_uuid(), x.to_uuid()],
        }
    }

    pub fn get_transaction_id(&self) -> &str {
        match self {
            AN(x) => x.transaction_id(),
            RN(x) => x.transaction_id(),
            ANP(x) => x.transaction_id(),
            RNP(x) => x.transaction_id(),
            AL(x) => x.transaction_id(),
            RL(x) => x.transaction_id(),
            AR(x) => x.transaction_id(),
            ARP(x) => x.transaction_id(),
            RRP(x) => x.transaction_id(),
        }
    }
}

///Error msg
encoding_struct! {
    ///Error msg
    struct ErrorMsg {
        ///message itself.
        msg: &str,
    }
}

///Relation struct
encoding_struct! {
    ///Relation struct
    struct Relation {
        ///Start node
        start_node_uuid: &str,
        ///End node
        end_node_uuid: &str
    }
}

///Our queries structure. This represents a set of queries for a single transaction
/// It has related transaction hash and in case of error, the appropriate message.
encoding_struct! {
    ///Queries struct
    struct Neo4jTransaction {
        ///queries themselves
        queries: &str,
        ///error from the database if any.
        error_msg: &str,
        ///Result, empty when not processed, otherwise either FAILURE or SUCCESS
        result: &str,
        ///Public key of the transaction initiator
        pub_key: &PublicKey
    }
}
