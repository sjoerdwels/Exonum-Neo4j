#!/bin/bash
node=$1
node_count=$2
node_cfg=$node"_cfg"
num=$((node+1))
pubNodes=""
target=$3
for i in $(seq 1 $((node_count)))
do
    path="../../shared-config/pub_$i.toml"
    pubNodes="$pubNodes $path"
done
echo target/$target/exonum-neo4j finalize --public-api-address 172.17.0.$num:8200 --private-api-address 172.17.0.$num:8091 sec_$node.toml node_$node_cfg.toml --public-configs $pubNodes
target/$target/exonum-neo4j finalize --public-api-address 172.17.0.$num:8200 --private-api-address 172.17.0.$num:8091 sec_$node.toml node_$node_cfg.toml --public-configs $pubNodes