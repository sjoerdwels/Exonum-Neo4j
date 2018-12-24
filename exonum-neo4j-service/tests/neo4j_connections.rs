extern crate exonum_neo4j;
extern crate exonum;
extern crate grpc;

// Import datatypes used in tests from the crate where the service is defined.

use exonum_neo4j::proto::TransactionManager;
use exonum_neo4j::neo4j_client::{get_client, get_commit_transaction_request};
use exonum_neo4j::proto::Status;

mod transaction_test_server;


#[test]
fn test_connection() {
    let server = transaction_test_server::TestServer::new(50051);

    let client = get_client(50051);

    let queries = "CREATE n:testPerson {name: 'exonumTest', age: 24 }";
    let req = get_commit_transaction_request(queries, "");
    let resp = client.verify(grpc::RequestOptions::new(), req);
    let answer = resp.wait();
    match answer {
        Ok(x) => {println!("Got OK result {:?}", x.1); assert!(true, true)},
        Err(e) => println!("error parsing header: {:?}", e),
    }
    let req = get_commit_transaction_request(queries, "hashahsahs");
    let resp = client.execute(grpc::RequestOptions::new(), req);
    let answer = resp.wait();

    match answer {
        Ok(x) => println!("Got OK result {:?}", x.1),
        _ => {println!("Got error "); assert!(true, false)}
    }
    server.end_server();

}

#[test]
fn test_failure() {
    let server = transaction_test_server::TestServer::new(50052);

    let client = get_client(50052);

    let queries = "abort;Create (n)";

    let req = get_commit_transaction_request(queries, "hashahsahs");
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
    server.end_server();
}