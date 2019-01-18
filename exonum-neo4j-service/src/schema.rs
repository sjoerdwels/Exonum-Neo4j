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

//! Cryptocurrency database schema.

use exonum::{
    crypto::{hash, Hash}, storage::{Fork, MapIndex, ListIndex, ProofListIndex, ProofMapIndex, Snapshot,},
};

use std::string::String;

use structures::{Neo4jTransaction, NodeChange, Relation};


/// Database schema for the cryptocurrency.
#[derive(Debug)]
pub struct Schema<T> {
    view: T,
}

impl<T> AsMut<T> for Schema<T> {
    fn as_mut(&mut self) -> &mut T {
        &mut self.view
    }
}

impl<T> Schema<T>
where
    T: AsRef<dyn Snapshot>,
{
    /// Creates a new schema from the database view.
    pub fn new(view: T) -> Self {
        Schema { view }
    }

    /// Returns `ProofMapIndex` with queries.
    pub fn neo4j_transactions(&self) -> ProofMapIndex<&T, Hash, Neo4jTransaction> {
        ProofMapIndex::new("neo4j.queries", &self.view)
    }

    /// Return queries in an ordered list
    pub fn neo4j_transactions_ordered(&self) -> ProofListIndex<&T,Hash> {
        ProofListIndex::new("neo4j.queries_ordered", &self.view)
    }

    pub fn get_last_confirmed_block(&self) -> Option<Hash> {
        let index : MapIndex<&T, String, Hash> = MapIndex::new("neo4j.values", &self.view);
        index.get(&String::from("lastConfirmedBlock"))
    }

    ///Get a single query, by giving transaction hash as key
    pub fn neo4j_transaction(&self, hash: &Hash) -> Option<Neo4jTransaction> {
        self.neo4j_transactions().get(hash)
    }

    ///Get relations ProofMapIndex
    pub fn relations(&self) -> ProofMapIndex<&T, Hash, Relation> {
        ProofMapIndex::new("neo4j.relations", &self.view)
    }

    ///Get a single relation, by giving it's uuid as key
    pub fn relation(&self, relation_uuid: &str) -> Option<Relation> {
        self.relations().get(&hash(relation_uuid.as_bytes()))
    }

    ///Get a node's history proofListIndex by giving that node's uuid.
    pub fn node_history(&self, node_name: &str) -> ProofListIndex<&T, NodeChange> {
        ProofListIndex::new(format!("neo4j.node_changes_{}", node_name), &self.view)

    }

    ///Get blocks that were audited by a Audit transaction.
    pub fn audited_blocks(&self, transaction_hash: &Hash) -> ListIndex<&T, Hash> {
        ListIndex::new(format!("neo4j.audited_block_{}", transaction_hash.to_hex().as_str()), &self.view)
    }

    ///Get state hash
    pub fn state_hash(&self) -> Vec<Hash> {
        vec![self.neo4j_transactions().merkle_root()]
    }
}

/// Implementation of mutable methods.
impl<'a> Schema<&'a mut Fork> {
    ///Get all queries from database.
    pub fn neo4j_transactions_mut(&mut self) -> ProofMapIndex<&mut Fork, Hash, Neo4jTransaction> {
        ProofMapIndex::new("neo4j.queries", &mut self.view)
    }

        ///Get a mutable prooflistindex for a node's history
    pub fn neo4j_transaction_ordered_mut(&mut self) -> ProofListIndex<&mut Fork, Hash> {
        ProofListIndex::new("neo4j.queries_ordered", &mut self.view)
    }

    ///Add a new variable to the table.
    pub fn add_neo4j_transaction(&mut self, q: Neo4jTransaction, hash : &Hash) {
        self.neo4j_transactions_mut().put(hash, q);
        self.neo4j_transaction_ordered_mut().push(hash.clone());
    }

    pub fn update_neo4j_transaction(&mut self, hash: &Hash, error_msg: &str, result: &str) {
        match self.neo4j_transaction(hash) {
            Some(neo4j_transaction) => {
                let updated_transaction = Neo4jTransaction::new(neo4j_transaction.queries(), error_msg, result);
                self.neo4j_transactions_mut().put(hash, updated_transaction);
            },
            None => {}
        }
    }

    ///Sets last confirmed block, so that we will not try to retrieve changes for that and before anymore.
    pub fn set_last_confirmed_block(&mut self, block_hash : Hash) {
        let mut index : MapIndex<&mut Fork, String, Hash> = MapIndex::new("neo4j.values", &mut self.view);
        let i_str = String::from("lastConfirmedBlock");
        if index.contains(&i_str) {
            index.remove(&i_str);
        }
        index.put(&i_str, block_hash);
    }

    ///Get mutable relations proofmapindex
    pub fn relations_mut(&mut self) -> ProofMapIndex<&mut Fork, Hash, Relation> {
        ProofMapIndex::new("neo4j.relations", &mut self.view)
    }

    ///Add a new variable to the table.
    pub fn add_relation(&mut self, r: Relation, relation_uuid : &str) {
        let hash = hash(relation_uuid.as_bytes());
        self.relations_mut().put(&hash, r);

    }

    ///Get a mutable prooflistindex for a node's history
    pub fn node_history_mut(&mut self, node_name: &str) -> ProofListIndex<&mut Fork, NodeChange> {
        ProofListIndex::new(format!("neo4j.node_changes_{}", node_name), &mut self.view)
    }

    ///Add to node history
    pub fn add_node_history(&mut self, uuid: &str, node_change: &NodeChange){
        self.node_history_mut(uuid).push(node_change.clone())
    }

    pub fn add_audited_block(&mut self, transaction_hash: &Hash, block_hash: Hash) {
        let mut index : ListIndex<&mut Fork, Hash> = ListIndex::new(format!("neo4j.audited_block_{}", transaction_hash.to_hex().as_str()), &mut self.view);
        index.push(block_hash);
    }
}
