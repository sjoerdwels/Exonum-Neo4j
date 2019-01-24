#!/bin/bash
echo target/debug/exonum-neo4j generate-template ../../shared-config/common.toml --validators-count $1
rm -r -f ../../shared-config/*
target/debug/exonum-neo4j generate-template ../../shared-config/common.toml --validators-count $1