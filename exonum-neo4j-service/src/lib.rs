// Copyright 2018 The Exonum Team
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! Cryptocurrency implementation example using [exonum](http://exonum.com/).

#![deny(
    missing_debug_implementations,
    missing_docs,
    unsafe_code,
    bare_trait_objects
)]

#[macro_use]
extern crate exonum;
extern crate serde;
#[macro_use]
extern crate failure;
#[macro_use]
extern crate serde_derive;

extern crate grpc;
extern crate protobuf;
extern crate tls_api;
extern crate tls_api_native_tls;
extern crate toml;



pub use schema::Schema;

///Our API.
pub mod api;
pub mod schema;
///Our possible transactions
pub mod transactions;
pub mod structures;
//Our gRPC client
pub mod gRPCProtocol;
pub mod gRPCProtocol_grpc;

pub mod util;


use transactions::Neo4JTransactions;

use exonum::{
    api::ServiceApiBuilder,
    blockchain::{self, Transaction, TransactionSet}, crypto::Hash,
    encoding::Error as EncodingError, helpers::fabric::{self, Context}, messages::RawTransaction,
    storage::Snapshot,
};

/// Unique service ID.
const NEO4J_SERVICE_ID: u16 = 144;
/// Name of the service.
const SERVICE_NAME: &str = "neo4J_blockchain";
/// Initial balance of the wallet.

/// Exonum `Service` implementation.
#[derive(Default, Debug)]
pub struct Service;

impl blockchain::Service for Service {
    fn service_id(&self) -> u16 {
        NEO4J_SERVICE_ID
    }

    fn service_name(&self) -> &str {
        SERVICE_NAME
    }

    fn state_hash(&self, view: &dyn Snapshot) -> Vec<Hash> {
        let schema = Schema::new(view);
        schema.state_hash()
    }

    fn tx_from_raw(&self, raw: RawTransaction) -> Result<Box<dyn Transaction>, EncodingError> {
        Neo4JTransactions::tx_from_raw(raw).map(Into::into)
    }

    fn wire_api(&self, builder: &mut ServiceApiBuilder) {
        api::Neo4JApi::wire(builder);
    }
}

/// A configuration service creator for the `NodeBuilder`.
#[derive(Debug)]
pub struct ServiceFactory;

impl fabric::ServiceFactory for ServiceFactory {
    fn service_name(&self) -> &str {
        SERVICE_NAME
    }

    fn make_service(&mut self, _: &Context) -> Box<dyn blockchain::Service> {
        match util::parse_port() {
            Ok(x) => println!("All good, neo4j port is {}", x),
            Err(e) => println!("error: {:?}", e),
        };
        Box::new(Service)
    }
}//So this is something that should construct the service?
