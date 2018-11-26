extern crate protobuf;

use exonum::crypto::Hash;


encoding_struct! {
    ///Our test variable, which we are going to change.
    struct Queries {
        queries: &str,
        transaction_hash: &Hash
    }
}

encoding_struct! {
    struct NodeChange {
        node_name: &str,
        what_changed: &str,
        old_value: &str,
        new_value: &str,
    }
}

pub fn getProtoBufList(queries: &str) -> protobuf::RepeatedField<::std::string::String> {
    let split = queries.split(";");
    let vec: Vec<::std::string::String> = split.map(|s| s.to_string()).collect();
    protobuf::RepeatedField::from_vec(vec)
}

impl Queries {
    fn getProtoList(self) -> protobuf::RepeatedField<::std::string::String> {
        let split = self.queries().split(";");
        let vec: Vec<::std::string::String> = split.map(|s| s.to_string()).collect();
        protobuf::RepeatedField::from_vec(vec)
    }
    

    pub fn execute(&self) -> Vec<NodeChange> {
        let c1 = NodeChange::new(
            "u1",
            "name",
            "",
            "test"
        );
        vec![c1]
    }
}

impl NodeChange {
    pub fn get_name(&self) -> &str{
        self.node_name().clone()
    }
}