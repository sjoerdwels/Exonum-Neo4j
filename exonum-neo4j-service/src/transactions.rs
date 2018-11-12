#![allow(bare_trait_objects)]

use exonum::{
    blockchain::{ExecutionResult, Transaction},
    messages::{Message}, storage::{Fork},
    crypto::{CryptoHash}
};

use schema::Schema;
use TEST_SERVICE_ID;



transactions! {
    /// Transaction group.
    pub TestTransactions {
        const SERVICE_ID = TEST_SERVICE_ID; // Remove this when updating.
        // Transfer `amount` of the currency from one wallet to another.
        struct ChangeValue {
            name: &str,
            amount:  u64,
        }

        struct NewValue {
            name: &str
        }
    }
}


impl Transaction for ChangeValue {
    fn verify(&self) -> bool {
        true
    }

    fn execute(&self, fork: &mut Fork) -> ExecutionResult {
        let hash = self.hash();

        let mut schema = Schema::new(fork);
        let amount = self.amount();
        let name = self.name();

        schema.set_value(name, amount, &hash);

        Ok(())
    }
}

impl Transaction for NewValue {

    fn verify(&self) -> bool {
        true
    }
    fn execute(&self, fork: &mut Fork) -> ExecutionResult {
        let hash = self.hash();

        let mut schema = Schema::new(fork);
        let name = self.name();

        schema.create_value(name, &hash);

        Ok(())
    }
}