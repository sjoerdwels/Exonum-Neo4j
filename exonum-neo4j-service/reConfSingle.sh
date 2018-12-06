rm -R example
mkdir example
exonum-neo4j generate-template example/common.toml --validators-count 1

exonum-neo4j generate-config example/common.toml  example/pub_1.toml example/sec_1.toml --peer-address 127.0.0.1:6331

exonum-neo4j finalize --public-api-address 0.0.0.0:8200 --private-api-address 0.0.0.0:8091 example/sec_1.toml example/node_1_cfg.toml --public-configs example/pub_1.toml
