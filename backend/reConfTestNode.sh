#!/bin/bash
node=$1

echo target/debug/exonum-neo4j generate-config ../../shared-config/common.toml  ../../shared-config/pub_node.toml ../../shared-config/sec_node.toml --peer-address 172.17.0.2:6331
target/debug/exonum-neo4j generate-config ../../shared-config/common.toml  ../../shared-config/pub_node.toml ../../shared-config/sec_node.toml --peer-address 172.17.0.2:6331
