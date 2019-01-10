extern crate grpc;
extern crate protobuf;
extern crate tls_api_native_tls;
extern crate exonum_neo4j;

use self::grpc::{ServerBuilder, RequestOptions, SingleResponse};
use self::protobuf::RepeatedField;
use self::tls_api_native_tls::TlsAcceptor;
use exonum_neo4j::proto::{TransactionManager, TransactionManagerServer, TransactionRequest, DatabaseModifications, TransactionResponse, Status, DatabaseModifications_CreatedNode};

use std::thread;

struct TransactionTestServerImpl;
use std::sync::mpsc::{self, Receiver, Sender, TryRecvError};



impl TransactionManager for TransactionTestServerImpl {
    fn verify(&self, _o: RequestOptions, p: TransactionRequest) -> SingleResponse<TransactionResponse> {
        let mut r = TransactionResponse::new();
        let queries = p.get_queries();
        println!("Got queries {:?}", queries);
        println!("Comparison value is {}", queries[0].trim()=="abort");
        if queries[0].trim()=="abort" {
            r.set_result(Status::FAILURE);
            println!("Setting result to FAILURE!");
        }
        else {
            r.set_result(Status::SUCCESS);
        }
        SingleResponse::completed(r)
    }

    fn execute(&self, _o: RequestOptions, p: TransactionRequest) -> SingleResponse<TransactionResponse> {
        let mut r = TransactionResponse::new();
        let queries = p.get_queries();
        let mut modifications : DatabaseModifications = DatabaseModifications::new();
        let mut new_nodes : RepeatedField<DatabaseModifications_CreatedNode> = RepeatedField::new();
        let mut node_a = DatabaseModifications_CreatedNode::new();
        node_a.set_node_UUID("u1".to_string());
        new_nodes.push( node_a);
        let mut node_a = DatabaseModifications_CreatedNode::new();
        node_a.set_node_UUID("u2".to_string());
        new_nodes.push( node_a);

        println!("TEST2 Got queries {:?}", queries);
        println!("Comparison value is {}", queries[0].trim()=="abort");
        if queries[0].trim()=="abort" {
            r.set_result(Status::FAILURE);
            println!("Setting result to FAILURE!");
        }
        else {
            r.set_result(Status::SUCCESS);
        }

        modifications.set_created_nodes(new_nodes);
        r.set_modifications(modifications);
        SingleResponse::completed(r)
    }
}

pub struct TestServer {
    sdr : Sender<u8>,
    //thr : &'a Thread,
}

impl TestServer {
    fn run_server(rcv : Receiver<u8>, port : u16) {
        let port = port;

        let mut server:ServerBuilder<TlsAcceptor> = ServerBuilder::new();
        server.http.set_port(port);
        server.add_service(TransactionManagerServer::new_service_def(TransactionTestServerImpl));
        server.http.set_cpu_pool_threads(1);
        let _server = server.build().expect("server");

        println!(
            "transaction server started on port {}",
            port,
        );

        loop {
            thread::park();
            match rcv.try_recv() {
                Ok(1) | Err(TryRecvError::Disconnected) => {
                    println!("Terminating.");
                    break;
                },
                Ok(_) => {},
                Err(TryRecvError::Empty) => {}
            }
        }
    }

    pub fn new(port : u16) -> TestServer {
        let (sdr, rcv) = mpsc::channel();
        //let thr : JoinChannel<T> =
        thread::spawn(move || { TestServer::run_server(rcv, port)});
        let new_server = TestServer{sdr};
        new_server
    }

    pub fn end_server(&self) {
        match self.sdr.send(1) {
            _ => {},
        };
        //Self.thr.unpark();
    }
}




