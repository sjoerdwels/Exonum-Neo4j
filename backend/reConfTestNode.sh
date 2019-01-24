#!/bin/bash
nodetoml=$1+".toml"

echo target/debug/exonum-neo4j generate-config ../../shared-config/common.toml  ../../shared-config/pub_$nodetoml ../../shared-config/sec_$nodetoml --peer-address 172.17.0.2:6331
target/debug/exonum-neo4j generate-config ../../shared-config/common.toml  ../../shared-config/pub_$nodetoml ../../shared-config/sec_$nodetoml --peer-address 172.17.0.2:6331