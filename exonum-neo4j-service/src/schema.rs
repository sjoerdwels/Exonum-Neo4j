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
    crypto::{hash, Hash}, storage::{StorageKey, Fork, ProofMapIndex, Snapshot, proof_map_index::{ProofMapKey},},
};

use testValue::TestValue;
use INITIAL_VALUE;

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

    /// Returns `ProofMapIndex` with wallets.
    pub fn values(&self) -> ProofMapIndex<&T, Hash, TestValue> {
        ProofMapIndex::new("test.values", &self.view)
    }

    ///Get a single variable, by giving variable name as key.
    pub fn value(&self, name: &str) -> Option<TestValue> {
        self.values().get(&hash(name.as_bytes()))
    }

    ///Get state hash
    pub fn state_hash(&self) -> Vec<Hash> {
        vec![self.values().merkle_root()]
    }
}

/// Implementation of mutable methods.
impl<'a> Schema<&'a mut Fork> {
    ///Get all variables from database.
    pub fn values_mut(&mut self) -> ProofMapIndex<&mut Fork, Hash, TestValue> {
        ProofMapIndex::new("test.values", &mut self.view)
    }

    ///Change the value of existing variable.
    pub fn set_value(&mut self, name: &str, newValue: u64, transaction: &Hash) {
        let value = self.value(name);
        match value {
            Some(value) => {
                let value = value.set_value(newValue);
                self.values_mut().put(&hash(value.name().as_bytes()), value);
            },
            None => ()

        }

    }

    ///Add a new variable to the table.
    pub fn create_value(&mut self, name: &str, transaction: &Hash) {
        let value = TestValue::new(name, INITIAL_VALUE);
        self.values_mut().put(&hash(name.as_bytes()), value);
    }
}
