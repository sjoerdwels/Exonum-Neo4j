extern crate exonum;
extern crate exonum_configuration;
extern crate exonum_neo4j;


use exonum::helpers::{self, fabric::NodeBuilder};
use exonum_configuration as configuration;
use exonum_neo4j as neo4j_service;
use exonum_neo4j::gRPCProtocol_grpc::run_server;
use std::thread;


fn main() {
    thread::spawn(move || {run_server()});//TODO remove it once live Neo4J server is working.

    exonum::crypto::init();
    helpers::init_logger().unwrap();

    let node = NodeBuilder::new()
        .with_service(Box::new(configuration::ServiceFactory))
        .with_service(Box::new(neo4j_service::ServiceFactory));
    node.run();
}
