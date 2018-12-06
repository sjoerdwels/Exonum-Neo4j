extern crate exonum_neo4j;
extern crate exonum;
extern crate grpc;

use exonum::crypto::{hash};
// Import datatypes used in tests from the crate where the service is defined.
use exonum_neo4j::gRPCProtocol_grpc::{getClient, run_server};
use exonum_neo4j::gRPCProtocol_grpc::TransactionManager;
use exonum_neo4j::gRPCProtocol::{TransactionRequest};
use exonum_neo4j::structures::Queries;
use std::thread;


#[test]
fn test_connection() {
    thread::spawn(move || {run_server()});

    let client = getClient(50051);

    let queries = Queries::new("CREATE n:testPerson {name: 'exonumTest', age: 24 }", &hash(b"hash"));
    let mut req = TransactionRequest::new();
    req.set_UUID_prefix("u1".to_string());
    req.set_queries(queries.clone().getProtoList());
    let resp = client.verify_transaction(grpc::RequestOptions::new(), req);
    let answer = resp.wait();
    match answer {
        Ok(x) => println!("Got OK result {:?}", x.1),
        _ => println!("Got error ")
    }

    let mut req = TransactionRequest::new();
    req.set_UUID_prefix("u1".to_string());
    req.set_queries(queries.clone().getProtoList());
    let resp = client.execute_transaction(grpc::RequestOptions::new(), req);
    let answer = resp.wait();
    match answer {
        Ok(x) => println!("Got OK result {:?}", x.1),
        _ => println!("Got error ")
    }


}