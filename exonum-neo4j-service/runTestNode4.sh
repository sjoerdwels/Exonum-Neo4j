#!/bin/bash
target/debug/exonum-neo4j run --node-config ../../shared-config/node_4_cfg.toml --db-path ../../shared-config/db4 --public-api-address 172.17.0.5:8200 >> exo_neo.log

