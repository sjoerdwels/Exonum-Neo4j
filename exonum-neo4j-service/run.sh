screen -t node1 ./target/debug/exonum-neo4j run --node-config example/node_1_cfg.toml --db-path example/db1 --public-api-address 0.0.0.0:8200
screen -t node2 ./target/debug/exonum-neo4j run --node-config example/node_2_cfg.toml --db-path example/db2 --public-api-address 0.0.0.0:8201
screen -t node3 ./target/debug/exonum-neo4j run --node-config example/node_3_cfg.toml --db-path example/db3 --public-api-address 0.0.0.0:8202
screen -t node4 ./target/debug/exonum-neo4j run --node-config example/node_4_cfg.toml --db-path example/db4 --public-api-address 0.0.0.0:8203

