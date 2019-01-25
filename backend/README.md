#Exonum Neo4j service

#### Requirements

- Cargo 
- Screen
- Protobuf (protoc in $PATH)


Requirements: Install screen, run*.sh requires that.

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

You first have to generate common.toml using, and provide number of nodes as X

exonum-neo4j generate-template path.../common.toml --validators-count $X

common.toml has to be shared to all of the validator nodes before next step.
Then in each node you have to run, replace X with the index of the node, and public address with the address, which other nodes use to access this node:

exonum-neo4j generate-config path.../common.toml  path.../pub_$X.toml sec_$X.toml --peer-address $public_address:6331

common.toml is an input, while pub and sec files are generated. All pub files have to be shared to other nodes before next step.
Finally you call finalize, for which you provide all the pub files of all the nodes.

exonum-neo4j finalize --public-api-address $public_address:8200 --private-api-address $public_address:8091 path.../sec_$X.toml path.../node_$X_cfg.toml --public-configs path.../pub_1.toml path.../pub_2.toml ...

Make sure in neo4j.toml at each node you have to correct port for the local Neo4j. By default it is 9994. This is the port for gRPC listener, which is provided by our extension. 
This ends the nodes configuration.

To start the node run:

exonum-neo4j run --node-config node_$X_cfg.toml --db-path path.../db$X --public-api-address $public_address:8200

You can choose the value for path... as you wish, as long as you use the same path for same files (pub, sec, node_cfg, common) and folder(db).

reConfSingle.sh and runSingle.sh is an example to setup a single node.
genCommonConfigTesting.sh, reConfTestNode.sh, finalizeTesting.sh and runTestNode.sh are used by the docker example to setup N docker containers which each act as a single node.