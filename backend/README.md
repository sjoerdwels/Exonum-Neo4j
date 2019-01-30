#Exonum Neo4j service

#### Requirements

- Cargo 
- Screen
- Protobuf (protoc in $PATH)

Exonum service installation:

apt-get install
	cargo
	build-essential
	pkg-config
	libssl-dev
	curl
Go to backend folder:
cargo build
cargo install

####Setup overview

* You first have to generate common.toml using, and provide number of nodes as X
    * exonum-neo4j generate-template path.../common.toml --validators-count $X
    * common.toml has to be shared to all of the validator nodes before next step.
* Then in each node you have to run, replace X with the index of the node, and public address with the address, which other nodes use to access this node:
    * exonum-neo4j generate-config path.../common.toml  path.../pub_$X.toml sec_$X.toml --peer-address $public_address:6331
    * common.toml is an input, while pub and sec files are generated. All pub files have to be shared to other nodes before next step.
* Finally you call finalize, for which you provide all the pub files of all the nodes.
    * exonum-neo4j finalize --public-api-address $public_address:8200 --private-api-address $public_address:8091 path.../sec_$X.toml path.../node_$X_cfg.toml --public-configs path.../pub_1.toml path.../pub_2.toml ...
    * Make sure in neo4j.toml at each node you have to correct port for the local Neo4j. By default it is 9994. This is the port for gRPC listener, which is provided by our extension. 
* This ends the nodes configuration. To start the node run:
    * exonum-neo4j run --node-config node_$X_cfg.toml --db-path path.../db$X --public-api-address $public_address:8200
    * You can choose the value for path... as you wish, as long as you use the same path for same files (pub, sec, node_cfg, common) and folder(db).

reConfSingle.sh and runSingle.sh is an example to setup a single node.

genCommonConfigTesting.sh, reConfTestNode.sh, finalizeTesting.sh and runTestNode.sh are used by the docker example to setup N docker containers which each act as a single node.

####Fast testing
To fast test that your single node is working, you can use these curl calls as a base (unix only)

#####Example for inserting transactions.
curl -i -X POST -H "Content-Type: application/json"  127.0.0.1:8200/api/services/neo4j_blockchain/v1/insert_transaction -d $'{
  "body": {
    "queries": "CREATE (n:Person {name:\'John\', money:100}) RETURN n",
    "datetime": "12:00:31 13-OCT-2018",
    "pub_key": "89ee16f86330960a09cd224242e7c4627e33751b2949f2cfb2f5b1008340d1f0"
  },
  "protocol_version": 0,
  "service_id": 144,
  "message_id": 0,
  "signature":"9f684227f1de663774548b3db656bca685e085321e2b00b0e115679fd45443ef58a5abeb555ab3d5f7a3cd27955a2079e5fd486743f36515c8e5bea07992100b"
}'
#####Example requests for different Get calls. You need to change the values if you want to use them though.
curl -i -H "Content-Type: application/json" -X GET 127.0.0.1:8200/api/services/neo4j_blockchain/v1/transactions

curl -i -H "Content-Type: application/json" -X GET 127.0.0.1:8200/api/services/neo4j_blockchain/v1/last5_transactions

curl -i -H "Content-Type: application/json" -X GET 127.0.0.1:8200/api/services/neo4j_blockchain/v1/node_history?node_uuid=d1f6a5b8303eceb61b9e87c7dc686ea179c1853f79345e1ccec9a9f914ca4e60_0

curl -i -H "Content-Type: application/json" -X GET 127.0.0.1:8200/api/services/neo4j_blockchain/v1/transaction?hash_string=71afce3e6a18a05376fccf766bfba321aa801af0ea6aef1a07b30e521363b3f8