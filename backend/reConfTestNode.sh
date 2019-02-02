#!/bin/bash
node=$1
target=$2


echo target/$target/exonum-neo4j generate-config ../../shared-config/common.toml  ../../shared-config/pub_$node.toml sec_$node.toml --peer-address 172.17.0.$((node+1)):6331
target/$target/exonum-neo4j generate-config ../../shared-config/common.toml  ../../shared-config/pub_$node.toml sec_$node.toml --peer-address 172.17.0.$((node+1)):6331
