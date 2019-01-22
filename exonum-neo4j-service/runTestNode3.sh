#!/bin/bash
target/debug/exonum-neo4j run --node-config ../../shared-config/node_3_cfg.toml --db-path ../../shared-config/db3 --public-api-address 172.17.0.4:8200 &

