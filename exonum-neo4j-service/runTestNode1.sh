#!/bin/bash
target/debug/exonum-neo4j run --node-config ../../shared-config/node_1_cfg.toml --db-path ../../shared-config/db1 --public-api-address 172.17.0.2:8200 &

