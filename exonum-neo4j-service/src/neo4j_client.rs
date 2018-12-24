// TODO all Neo4j service all, convert  function, in this file. Do not spread all Neo4j access logic
// in different files.

// todo: fix imports
use std::net::SocketAddr;
use std::sync::Arc;
use std::fmt;
use std::thread;
use grpc::{ServerBuilder, Client, ClientStub, RequestOptions, SingleResponse};
use grpc::rt::{MethodDescriptor, GrpcStreaming, ServerServiceDefinition, ServerMethod, MethodHandlerUnary};
use grpc::protobuf::{MarshallerProtobuf};
use protobuf::RepeatedField;
use tls_api_native_tls::TlsAcceptor;

use proto::{TransactionManager, TransactionManagerClient, TransactionRequest, DatabaseModifications, TransactionResponse, Status, DatabaseModifications_CreatedNode};

// Get client
pub fn get_client(port : u16) -> TransactionManagerClient {
    let client_conf = Default::default();
    let grpc_client = Arc::new(Client::new_plain("127.0.0.1", port, client_conf).unwrap());
    let client = TransactionManagerClient::with_client(grpc_client);
    client
}


///This generates a protobuff message that we will send to neo4j
pub fn get_commit_transaction_request(queries: &str, prefix: &str) -> TransactionRequest {
    let split = queries.split(";");
    let vec: Vec<::std::string::String> = split.map(|s| s.to_string()).collect();
    let proto_vec = protobuf::RepeatedField::from_vec(vec);
    let mut req = TransactionRequest::new();
    req.set_UUID_prefix(prefix.to_string());
    req.set_queries(proto_vec);
    req
}
