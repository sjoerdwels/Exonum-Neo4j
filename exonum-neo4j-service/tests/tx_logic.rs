extern crate exonum;
extern crate exonum_neo4j;
#[macro_use] extern crate exonum_testkit;

use exonum::crypto::{self};
use exonum_testkit::{TestKit, TestKitBuilder};
// Import datatypes used in tests from the crate where the service is defined.
use exonum_neo4j::schema::Schema;
use exonum_neo4j::transactions::CommitQueries;
use exonum_neo4j::Service;

mod transaction_test_server;

fn init_testkit() -> TestKit {
    TestKitBuilder::validator()
        .with_service(Service)
        .create()
}

#[test]
fn test_wrong_query() {
    let server = transaction_test_server::TestServer::new(50051);

    let mut testkit = init_testkit();
    let (_pubkey, key) = crypto::gen_keypair();
    testkit.create_block_with_transactions(txvec![
        CommitQueries::new("abort;CREAT (n)", &key),
    ]);
    let snapshot = testkit.snapshot();
    let schema = Schema::new(&snapshot);
    let queries = schema.queries();

    assert_eq!(queries.values().count(), 1);
}


#[test]
fn test_commit_query() {
    let server = transaction_test_server::TestServer::new(50052);

    let mut testkit = init_testkit();
    let snapshot = testkit.snapshot();
    let schema = Schema::new(&snapshot);
    let queries = schema.queries();

    assert_eq!(queries.values().count(), 0);
    let (_pubkey, key) = crypto::gen_keypair();
    testkit.create_block_with_transactions(txvec![
        CommitQueries::new("INSERT something", &key),
    ]);
    let snapshot = testkit.snapshot();
    let schema = Schema::new(&snapshot);
    let queries = schema.queries();
    let test_node_changes = schema.node_history("u1");

    match queries.values().last(){
        Some(x) => assert_eq!(x.queries(), "INSERT something"),
        None => panic!("Null query found")
    }
    assert_eq!(queries.values().count(), 1);
    assert_eq!(test_node_changes.len(), 1);
}

