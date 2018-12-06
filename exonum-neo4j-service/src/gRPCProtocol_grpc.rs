// This file is generated. Do not edit
// @generated

// https://github.com/Manishearth/rust-clippy/issues/702
#![allow(unknown_lints)]
#![allow(clippy)]

#![cfg_attr(rustfmt, rustfmt_skip)]

#![allow(box_pointers)]
#![allow(dead_code)]
#![allow(missing_docs)]
#![allow(non_camel_case_types)]
#![allow(non_snake_case)]
#![allow(non_upper_case_globals)]
#![allow(trivial_casts)]
#![allow(unsafe_code)]
#![allow(unused_imports)]
#![allow(unused_results)]

use std::net::SocketAddr;
use std::sync::Arc;
use std::fmt;
use std::thread;


use grpc::{ServerBuilder, Client, ClientStub, RequestOptions, SingleResponse};
use grpc::rt::{MethodDescriptor, GrpcStreaming, ServerServiceDefinition, ServerMethod, MethodHandlerUnary};
use grpc::protobuf::{MarshallerProtobuf};
use protobuf::RepeatedField;
use gRPCProtocol::{TransactionRequest, TransactionResponse, Status, DatabaseModifications, DatabaseModifications_CreatedNode};
use tls_api_native_tls::TlsAcceptor;



// interface

pub trait TransactionManager {
    fn verify_transaction(&self, o: RequestOptions, p: TransactionRequest) -> SingleResponse<TransactionResponse>;

    fn execute_transaction(&self, o: RequestOptions, p: TransactionRequest) -> SingleResponse<TransactionResponse>;
}

// client
pub struct TransactionManagerClient {
    grpc_client: ::std::sync::Arc<Client>,
    method_VerifyTransaction: Arc<MethodDescriptor<TransactionRequest, TransactionResponse>>,
    method_ExecuteTransaction: Arc<MethodDescriptor<TransactionRequest, TransactionResponse>>,
}

impl fmt::Debug for TransactionManagerClient {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "TransactionManagerClient...")
    }
}

impl ClientStub for TransactionManagerClient {
    fn with_client(grpc_client: Arc<Client>) -> Self {
        TransactionManagerClient {
            grpc_client: grpc_client,
            method_VerifyTransaction: ::std::sync::Arc::new(MethodDescriptor {
                name: "/protobuf.TransactionManager/VerifyTransaction".to_string(),
                streaming: GrpcStreaming::Unary,
                req_marshaller: Box::new(MarshallerProtobuf),
                resp_marshaller: Box::new(MarshallerProtobuf),
            }),
            method_ExecuteTransaction: Arc::new(MethodDescriptor {
                name: "/protobuf.TransactionManager/ExecuteTransaction".to_string(),
                streaming: GrpcStreaming::Unary,
                req_marshaller: Box::new(MarshallerProtobuf),
                resp_marshaller: Box::new(MarshallerProtobuf),
            }),
        }
    }
}

impl TransactionManager for TransactionManagerClient {
    fn verify_transaction(&self, o: RequestOptions, p: TransactionRequest) -> SingleResponse<TransactionResponse> {
        self.grpc_client.call_unary(o, p, self.method_VerifyTransaction.clone())
    }

    fn execute_transaction(&self, o: RequestOptions, p: TransactionRequest) -> SingleResponse<TransactionResponse> {
        self.grpc_client.call_unary(o, p, self.method_ExecuteTransaction.clone())
    }
}

pub fn getClient(port : u16) -> TransactionManagerClient {
    let client_conf = Default::default();
    let grpc_client = Arc::new(Client::new_plain("127.0.0.1", port, client_conf).unwrap());
    let client = TransactionManagerClient::with_client(grpc_client);
    client
}

//Server code, only for testing
pub struct TransactionManagerServer;

impl fmt::Debug for TransactionManagerServer {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "TransactionManagerServer..")
    }
}


impl TransactionManagerServer {
    pub fn new_service_def<H : TransactionManager + 'static + Sync + Send + 'static>(handler: H) -> ServerServiceDefinition {
        let handler_arc = ::std::sync::Arc::new(handler);
        ServerServiceDefinition::new("/protobuf.TransactionManager",
            vec![
                ServerMethod::new(
                    ::std::sync::Arc::new(MethodDescriptor {
                        name: "/protobuf.TransactionManager/VerifyTransaction".to_string(),
                        streaming: GrpcStreaming::Unary,
                        req_marshaller: Box::new(MarshallerProtobuf),
                        resp_marshaller: Box::new(MarshallerProtobuf),
                    }),
                    {
                        let handler_copy = handler_arc.clone();
                        MethodHandlerUnary::new(move |o, p| handler_copy.verify_transaction(o, p))
                    },
                ),
                ServerMethod::new(
                    ::std::sync::Arc::new(MethodDescriptor {
                        name: "/protobuf.TransactionManager/ExecuteTransaction".to_string(),
                        streaming: GrpcStreaming::Unary,
                        req_marshaller: Box::new(MarshallerProtobuf),
                        resp_marshaller: Box::new(MarshallerProtobuf),
                    }),
                    {
                        let handler_copy = handler_arc.clone();
                        MethodHandlerUnary::new(move |o, p| handler_copy.execute_transaction(o, p))
                    },
                ),
            ],
        )
    }
}

struct TransactionImpl;

impl TransactionManager for TransactionImpl {
    fn verify_transaction(&self, _o: RequestOptions, p: TransactionRequest) -> SingleResponse<TransactionResponse> {
        let mut r = TransactionResponse::new();
        let queries = p.get_queries();
        let status : Status = Status::SUCCESS;
        println!("Got queries {:?}", queries);
        r.set_result(status);
        SingleResponse::completed(r)
    }

    fn execute_transaction(&self, _o: RequestOptions, p: TransactionRequest) -> SingleResponse<TransactionResponse> {
        let mut r = TransactionResponse::new();
        let queries = p.get_queries();
        let status : Status = Status::SUCCESS;
        let mut modifications : DatabaseModifications = DatabaseModifications::new();
        let mut new_nodes : RepeatedField<DatabaseModifications_CreatedNode> = RepeatedField::new();
        let mut nodeA = DatabaseModifications_CreatedNode::new();
        nodeA.set_node_UUID("u1".to_string());
        new_nodes.push( nodeA);
        let mut nodeA = DatabaseModifications_CreatedNode::new();
        nodeA.set_node_UUID("u2".to_string());
        new_nodes.push( nodeA);

        println!("Got queries {:?}", queries);
        r.set_result(status);
        modifications.set_created_nodes(new_nodes);
        r.set_modifications(modifications);
        SingleResponse::completed(r)
    }
}

pub fn run_server() {
    let port = 50051;

    let mut server:ServerBuilder<TlsAcceptor> = ServerBuilder::new();
    server.http.set_port(port);
    server.add_service(TransactionManagerServer::new_service_def(TransactionImpl));
    server.http.set_cpu_pool_threads(1);
    let _server = server.build().expect("server");

    println!(
        "transaction server started on port {}",
        port,
    );

    loop {
        thread::park();
    }
}