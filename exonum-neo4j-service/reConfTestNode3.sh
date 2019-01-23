#!/bin/bash

target/debug/exonum-neo4j generate-config ../../shared-config/common.toml  ../../shared-config/pub_3.toml ../../shared-config/sec_3.toml --peer-address 172.17.0.4:6331 >> exo_neo.log

