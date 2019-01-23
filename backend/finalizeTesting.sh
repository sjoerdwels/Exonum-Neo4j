#!/bin/bash

target/debug/exonum-neo4j finalize --public-api-address 172.17.0.2:8200 --private-api-address 172.17.0.2:8091 ../../shared-config/sec_1.toml ../../shared-config/node_1_cfg.toml --public-configs ../../shared-config/pub_1.toml ../../shared-config/pub_2.toml ../../shared-config/pub_3.toml ../../shared-config/pub_4.toml
target/debug/exonum-neo4j finalize --public-api-address 172.17.0.3:8200 --private-api-address 172.17.0.3:8091 ../../shared-config/sec_2.toml ../../shared-config/node_2_cfg.toml --public-configs ../../shared-config/pub_1.toml ../../shared-config/pub_2.toml ../../shared-config/pub_3.toml ../../shared-config/pub_4.toml
target/debug/exonum-neo4j finalize --public-api-address 172.17.0.4:8200 --private-api-address 172.17.0.4:8091 ../../shared-config/sec_3.toml ../../shared-config/node_3_cfg.toml --public-configs ../../shared-config/pub_1.toml ../../shared-config/pub_2.toml ../../shared-config/pub_3.toml ../../shared-config/pub_4.toml
target/debug/exonum-neo4j finalize --public-api-address 172.17.0.5:8200 --private-api-address 172.17.0.5:8091 ../../shared-config/sec_4.toml ../../shared-config/node_4_cfg.toml --public-configs ../../shared-config/pub_1.toml ../../shared-config/pub_2.toml ../../shared-config/pub_3.toml ../../shared-config/pub_4.toml

