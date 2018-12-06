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
use grpc::Client;
use grpc::ClientStub;
use std::fmt;

// interface

pub trait TransactionManager {
    fn verify_transaction(&self, o: ::grpc::RequestOptions, p: super::gRPCProtocol::TransactionRequest) -> ::grpc::SingleResponse<super::gRPCProtocol::TransactionResponse>;

    fn execute_transaction(&self, o: ::grpc::RequestOptions, p: super::gRPCProtocol::TransactionRequest) -> ::grpc::SingleResponse<super::gRPCProtocol::TransactionResponse>;
}

// client
pub struct TransactionManagerClient {
    grpc_client: ::std::sync::Arc<::grpc::Client>,
    method_VerifyTransaction: Arc<::grpc::rt::MethodDescriptor<super::gRPCProtocol::TransactionRequest, super::gRPCProtocol::TransactionResponse>>,
    method_ExecuteTransaction: Arc<::grpc::rt::MethodDescriptor<super::gRPCProtocol::TransactionRequest, super::gRPCProtocol::TransactionResponse>>,
}

impl fmt::Debug for TransactionManagerClient {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "TransactionManagerClient...")
    }
}

impl ::grpc::ClientStub for TransactionManagerClient {
    fn with_client(grpc_client: Arc<::grpc::Client>) -> Self {
        TransactionManagerClient {
            grpc_client: grpc_client,
            method_VerifyTransaction: ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                name: "/protobuf.TransactionManager/VerifyTransaction".to_string(),
                streaming: ::grpc::rt::GrpcStreaming::Unary,
                req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
            }),
            method_ExecuteTransaction: Arc::new(::grpc::rt::MethodDescriptor {
                name: "/protobuf.TransactionManager/ExecuteTransaction".to_string(),
                streaming: ::grpc::rt::GrpcStreaming::Unary,
                req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
            }),
        }
    }
}

impl TransactionManager for TransactionManagerClient {
    fn verify_transaction(&self, o: ::grpc::RequestOptions, p: super::gRPCProtocol::TransactionRequest) -> ::grpc::SingleResponse<super::gRPCProtocol::TransactionResponse> {
        self.grpc_client.call_unary(o, p, self.method_VerifyTransaction.clone())
    }

    fn execute_transaction(&self, o: ::grpc::RequestOptions, p: super::gRPCProtocol::TransactionRequest) -> ::grpc::SingleResponse<super::gRPCProtocol::TransactionResponse> {
        self.grpc_client.call_unary(o, p, self.method_ExecuteTransaction.clone())
    }
}

pub fn getClient() -> TransactionManagerClient {
    let port = 50051;
    let client_conf = Default::default();
    let grpc_client = Arc::new(Client::new_plain("127.0.0.1", port, client_conf).unwrap());
    let client = TransactionManagerClient::with_client(grpc_client);
    client
}
