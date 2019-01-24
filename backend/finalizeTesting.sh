#!/bin/bash
node=$1
node_count=$2
num=$((node+1))
pubNodes=""
for i in $(seq 1 $((node_count)))
do
    pubNodes=$pubNodes "../../shared-config/pub_$i.toml"
done
echo target/debug/exonum-neo4j finalize --public-api-address 172.17.0.$num:8200 --private-api-address 172.17.0.$num:8091 ../../shared-config/sec_$node.toml ../../shared-config/node_$node_cfg.toml --public-configs $pubNodes
target/debug/exonum-neo4j finalize --public-api-address 172.17.0.$num:8200 --private-api-address 172.17.0.$num:8091 ../../shared-config/sec_$node.toml ../../shared-config/node_$node_cfg.toml --public-configs $pubNodes