#!/bin/bash
i=$1
i_cfg=$i"_cfg"
echo target/debug/exonum-neo4j run --node-config node_$i_cfg.toml --db-path db$i --public-api-address 172.17.0.$((i+1)):8200 >> exo_neo.log
target/debug/exonum-neo4j run --node-config node_$i_cfg.toml --db-path db$i --public-api-address 172.17.0.$((i+1)):8200 >> exo_neo.log