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


// interface

pub trait TransactionManager {
    fn verify(&self, o: ::grpc::RequestOptions, p: super::gRPCProtocol::TransactionRequest) -> ::grpc::SingleResponse<super::gRPCProtocol::TransactionResponse>;

    fn execute(&self, o: ::grpc::RequestOptions, p: super::gRPCProtocol::TransactionRequest) -> ::grpc::SingleResponse<super::gRPCProtocol::TransactionResponse>;
}

// client

pub struct TransactionManagerClient {
    grpc_client: ::std::sync::Arc<::grpc::Client>,
    method_Verify: ::std::sync::Arc<::grpc::rt::MethodDescriptor<super::gRPCProtocol::TransactionRequest, super::gRPCProtocol::TransactionResponse>>,
    method_Execute: ::std::sync::Arc<::grpc::rt::MethodDescriptor<super::gRPCProtocol::TransactionRequest, super::gRPCProtocol::TransactionResponse>>,
}

impl ::grpc::ClientStub for TransactionManagerClient {
    fn with_client(grpc_client: ::std::sync::Arc<::grpc::Client>) -> Self {
        TransactionManagerClient {
            grpc_client: grpc_client,
            method_Verify: ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                name: "/protobuf.TransactionManager/Verify".to_string(),
                streaming: ::grpc::rt::GrpcStreaming::Unary,
                req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
            }),
            method_Execute: ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                name: "/protobuf.TransactionManager/Execute".to_string(),
                streaming: ::grpc::rt::GrpcStreaming::Unary,
                req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
            }),
        }
    }
}

impl TransactionManager for TransactionManagerClient {
    fn verify(&self, o: ::grpc::RequestOptions, p: super::gRPCProtocol::TransactionRequest) -> ::grpc::SingleResponse<super::gRPCProtocol::TransactionResponse> {
        self.grpc_client.call_unary(o, p, self.method_Verify.clone())
    }

    fn execute(&self, o: ::grpc::RequestOptions, p: super::gRPCProtocol::TransactionRequest) -> ::grpc::SingleResponse<super::gRPCProtocol::TransactionResponse> {
        self.grpc_client.call_unary(o, p, self.method_Execute.clone())
    }
}

// server

pub struct TransactionManagerServer;


impl TransactionManagerServer {
    pub fn new_service_def<H : TransactionManager + 'static + Sync + Send + 'static>(handler: H) -> ::grpc::rt::ServerServiceDefinition {
        let handler_arc = ::std::sync::Arc::new(handler);
        ::grpc::rt::ServerServiceDefinition::new("/protobuf.TransactionManager",
            vec![
                ::grpc::rt::ServerMethod::new(
                    ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                        name: "/protobuf.TransactionManager/Verify".to_string(),
                        streaming: ::grpc::rt::GrpcStreaming::Unary,
                        req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                        resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                    }),
                    {
                        let handler_copy = handler_arc.clone();
                        ::grpc::rt::MethodHandlerUnary::new(move |o, p| handler_copy.verify(o, p))
                    },
                ),
                ::grpc::rt::ServerMethod::new(
                    ::std::sync::Arc::new(::grpc::rt::MethodDescriptor {
                        name: "/protobuf.TransactionManager/Execute".to_string(),
                        streaming: ::grpc::rt::GrpcStreaming::Unary,
                        req_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                        resp_marshaller: Box::new(::grpc::protobuf::MarshallerProtobuf),
                    }),
                    {
                        let handler_copy = handler_arc.clone();
                        ::grpc::rt::MethodHandlerUnary::new(move |o, p| handler_copy.execute(o, p))
                    },
                ),
            ],
        )
    }
}
