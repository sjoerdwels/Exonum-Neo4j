
//! Module of the rust-protobuf generated files.

#![allow(bare_trait_objects)]
#![allow(renamed_and_removed_lints)]

pub use self::neo4j_service::*;

// Include generated protobuf files
include!(concat!(env!("OUT_DIR"), "/protobuf_mod.rs"));

// Use types from `exonum` .proto files.
// todo: enable when exonum is updated to v0.10.0
//use exonum::proto::schema::*;