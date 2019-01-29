extern crate exonum;
extern crate exonum_neo4j;
#[macro_use]
extern crate exonum_testkit;

use exonum::crypto;
use exonum_testkit::{TestKit, TestKitBuilder};
// Import datatypes used in tests from the crate where the service is defined.
use exonum_neo4j::neo4j;
use exonum_neo4j::schema::Schema;
use exonum_neo4j::structures::NodeChange;
use exonum_neo4j::transactions::{AuditBlocks, CommitQueries};
use exonum_neo4j::Neo4jService;

pub mod transaction_test_server;

fn init_testkit(port: u16) -> TestKit {
    let neo4j_config = neo4j::Neo4jConfig {
        address: String::from("127.0.0.1"),
        port: port,
    };

    let neo4j_rpc = neo4j::Neo4jRpc::new(neo4j_config);
    TestKitBuilder::validator()
        .with_service(Neo4jService::new(neo4j_rpc))
        .create()
}

#[test]
fn test_wrong_query() {
    let _server = transaction_test_server::TestServer::new(50051);

    let mut testkit = init_testkit(50051);
    let (_pubkey, key) = crypto::gen_keypair();
    testkit.create_block_with_transactions(txvec![CommitQueries::new(
        "abort;CREAT (n)",
        "15-OCT",
        &key
    ),]);
    let snapshot = testkit.snapshot();
    let schema = Schema::new(&snapshot);
    let queries = schema.neo4j_transactions();

    assert_eq!(queries.values().count(), 1);
}

#[test]
fn test_commit_query() {
    let _server = transaction_test_server::TestServer::new(50052);

    let mut testkit = init_testkit(50052);
    let snapshot = testkit.snapshot();
    let schema = Schema::new(&snapshot);
    let queries = schema.neo4j_transactions();

    assert_eq!(queries.values().count(), 0);
    let (_pubkey, key) = crypto::gen_keypair();
    testkit.create_block_with_transactions(txvec![CommitQueries::new(
        "INSERT something",
        "15-OCT",
        &key
    ),]);
    let snapshot = testkit.snapshot();
    let schema = Schema::new(&snapshot);
    let queries = schema.neo4j_transactions();

    match queries.values().last() {
        Some(x) => assert_eq!(x.queries(), "INSERT something"),
        None => panic!("Null query found"),
    }
    assert_eq!(queries.values().count(), 1);
}

#[test]
fn test_get_changes_query() {
    let _server = transaction_test_server::TestServer::new(9994);

    let mut testkit = init_testkit(9994);
    let (_pubkey, key) = crypto::gen_keypair();
    testkit.create_block_with_transactions(txvec![CommitQueries::new(
        "INSERT something",
        "15-OCT",
        &key
    ),]);
    testkit.create_block_with_transactions(txvec![AuditBlocks::new("43827394273", &key),]);
    let snapshot = testkit.snapshot();
    let schema = Schema::new(&snapshot);
    let node_changes = schema.node_history("u1");
    assert_eq!(node_changes.len(), 1);
    println!("{:?}", node_changes.get(0));
    match node_changes.get(0) {
        Some(node_change) => match node_change {
            NodeChange::AN(x) => assert_eq!(
                x.transaction_id(),
                "71afce3e6a18a05376fccf766bfba321aa801af0ea6aef1a07b30e521363b3f8".to_string()
            ),
            _ => assert_eq!(true, false),
        },
        _ => assert_eq!(true, false),
    }
}
