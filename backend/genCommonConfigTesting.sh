#!/bin/bash
echo target/debug/exonum-neo4j generate-template ../../shared-config/common.toml --validators-count $1
target/debug/exonum-neo4j generate-template ../../shared-config/common.toml --validators-count $1