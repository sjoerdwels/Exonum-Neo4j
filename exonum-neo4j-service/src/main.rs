extern crate exonum;
extern crate exonum_configuration;

use exonum::helpers::{self, fabric::NodeBuilder};
use exonum_configuration as configuration;

fn main() {
    exonum::crypto::init();
    helpers::init_logger().unwrap();

    let node = NodeBuilder::new()//When I pass the parameters related to conf then does this handle it somehwo?
        .with_service(Box::new(configuration::ServiceFactory))//So what does that do?
    node.run();
}
