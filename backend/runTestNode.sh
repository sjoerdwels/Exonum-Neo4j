#!/bin/bash
i=$1
target/debug/exonum-neo4j run --node-config ../../shared-config/node_$1_cfg.toml --db-path ../../shared-config/db$1 --public-api-address 172.17.0.$((i+1)):8200 >> exo_neo.log

