extern crate exonum;
extern crate exonum_neo4j;
#[macro_use] extern crate exonum_testkit;

use exonum::blockchain::Transaction;
use exonum::crypto::{self, PublicKey, SecretKey};
use exonum_testkit::{TestKit, TestKitBuilder};
// Import datatypes used in tests from the crate where the service is defined.
use exonum_neo4j::schema::Schema;
use exonum_neo4j::transactions::CommitQueries;
use exonum_neo4j::Service;

fn init_testkit() -> TestKit {
    TestKitBuilder::validator()
        .with_service(Service)
        .create()
}

#[test]
fn test_commit_query() {
    let mut testkit = init_testkit();
    let (pubkey, key) = crypto::gen_keypair();
    testkit.create_block_with_transactions(txvec![
        CommitQueries::new("INSERT something", &key),
    ]);
    let snapshot = testkit.snapshot();
    let schema = Schema::new(&snapshot);
    let queries = schema.queries();
    let testNodeChanges = schema.node_history("u1");

    match queries.values().last(){
        Some(x) => assert_eq!(x.queries(), "INSERT something"),
        None => panic!("Null query found")
    }
    assert_eq!(queries.values().count(), 1);
    assert_eq!(testNodeChanges.len(), 1);
}
