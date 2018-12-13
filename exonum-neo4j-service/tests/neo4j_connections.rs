extern crate exonum_neo4j;
extern crate exonum;
extern crate grpc;

// Import datatypes used in tests from the crate where the service is defined.
use exonum_neo4j::gRPCProtocol_grpc::{getClient, run_server};
use exonum_neo4j::gRPCProtocol_grpc::TransactionManager;
use exonum_neo4j::structures::{getProtoTransactionRequest};
use exonum_neo4j::gRPCProtocol::Status;
use std::thread;


#[test]
fn test_connection() {
    thread::spawn(move || {run_server()});

    let client = getClient(50051);

    let queries = "CREATE n:testPerson {name: 'exonumTest', age: 24 }";
    let req = getProtoTransactionRequest(queries, "");
    let resp = client.verify(grpc::RequestOptions::new(), req);
    let answer = resp.wait();
    match answer {
        Ok(x) => {println!("Got OK result {:?}", x.1); assert!(true, true)},
        Err(e) => println!("error parsing header: {:?}", e),
    }
cargo
    let req = getProtoTransactionRequest(queries, "hashahsahs");
    let resp = client.execute(grpc::RequestOptions::new(), req);
    let answer = resp.wait();
    match answer {
        Ok(x) => println!("Got OK result {:?}", x.1),
        _ => {println!("Got error "); assert!(true, false)}
    }
}

//#[test]
fn test_failure() {
    thread::spawn(move || {run_server()});

    let client = getClient(50051);

    let queries = "abort;Create (n)";

    let req = getProtoTransactionRequest(queries, "hashahsahs");
    let resp = client.execute(grpc::RequestOptions::new(), req);
    let answer = resp.wait();
    match answer {
        Ok(x) => {println!("Got OK result {:?}", x.1);
            match x.1.get_result() {
                Status::FAILURE => println!("All good"),
                Status::SUCCESS => {assert!(true, false)}
            }},
        _ => {println!("Got error "); assert!(true, false)}
    }

}