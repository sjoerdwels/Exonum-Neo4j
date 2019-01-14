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

// TODO FIX LINTER
#![allow(
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

pub mod api;
pub mod neo4j;
pub mod schema;
pub mod transactions;
pub mod structures;
pub mod util;

use transactions::Neo4JTransactions;
use transactions::AuditBlocks;

use exonum::{
    api::ServiceApiBuilder,
    blockchain::{self, Schema as CoreSchema, ServiceContext, Transaction, TransactionSet}, crypto::Hash,
    encoding::Error as EncodingError, helpers::fabric::{self, Context}, messages::RawTransaction,
    storage::Snapshot,
};

use neo4j::ExecuteResponse::*;

/// Unique service ID.
const NEO4J_SERVICE_ID: u16 = 144;
/// Name of the service.
const SERVICE_NAME: &str = "neo4j_blockchain";

/// Exonum `Neo4jService` implementation.
pub struct Neo4jService {
    neo4j : neo4j::Neo4jRpc
}

impl ::std::fmt::Debug for Neo4jService {
    fn fmt(&self, f: &mut ::std::fmt::Formatter) -> ::std::fmt::Result {
        f.debug_struct("Neo4jService").finish()
    }
}

impl Neo4jService {
    /// Creates  a Neo4j RPC service
    pub fn new( neo4j : neo4j::Neo4jRpc) -> Self {
        Self {neo4j}
    }
}

impl blockchain::Service for Neo4jService {
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

    fn after_commit(&self, context: &ServiceContext) {
        let snapshot = context.snapshot();
        let core_schema = CoreSchema::new(snapshot);
        let schema = Schema::new(snapshot);
        let last_block = core_schema.block_hashes_by_height().last();
        match last_block {
            Some(block_hash) => {
                let block_option = core_schema.blocks().get(&block_hash);
                match block_option {
                    Some(block) => {
                        let result = self.neo4j.execute_block(block, core_schema, schema);
                        match result {
                            OkExe(_) => {
                                let tx_sender = context.transaction_sender();
                                let new_tx = AuditBlocks::new(context.secret_key());
                                match tx_sender.send(Box::new(new_tx)) {
                                    _ => {}
                                };
                            },
                            _ => {} //No need to do anything
                        }
                    },
                    None => {}
                }

            },
            None => {} //TODO Error, should never get here though, as we always have a last block in an after_commit
        }

    }

    fn wire_api(&self, builder: &mut ServiceApiBuilder) {
        api::Neo4JApi::wire(builder);
    }
}

/// A configuration service creator for the `NodeBuilder`.
#[derive(Debug, Copy, Clone)]
pub struct Neo4jServiceFactory;

impl fabric::ServiceFactory for Neo4jServiceFactory {
    fn service_name(&self) -> &str {
        SERVICE_NAME
    }

    fn make_service(&mut self, _: &Context) -> Box<dyn blockchain::Service> {
        match util::parse_port() {
            Ok(x) => println!("All good, neo4j port is {}", x),
            Err(e) => println!("error: {:?}", e),
        };

        let neo4j_config = neo4j::Neo4jConfig{
            address : String::from("127.0.0.1"),
            port : 9994
        };

        let neo4j_rpc = neo4j::Neo4jRpc::new(neo4j_config);

        let service = Neo4jService::new(neo4j_rpc);

        Box::new(service)
    }
}
