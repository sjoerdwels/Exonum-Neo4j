extern crate exonum;
extern crate exonum_configuration;
extern crate exonum_neo4j;


use exonum::helpers::{self, fabric::NodeBuilder};
use exonum_configuration as configuration;
use exonum_neo4j as test_service;


fn main() {
    exonum::crypto::init();
    helpers::init_logger().unwrap();

    let node = NodeBuilder::new()
        .with_service(Box::new(configuration::ServiceFactory))
        .with_service(Box::new(test_service::ServiceFactory));
    node.run();
}
