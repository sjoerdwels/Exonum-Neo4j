#!/bin/bash
target/debug/exonum-neo4j run --node-config ../../shared-config/node_2_cfg.toml --db-path ../../shared-config/db2 --public-api-address 172.17.0.3:8200 >> exo_neo.log

