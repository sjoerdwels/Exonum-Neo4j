#!/bin/bash
target = $2
echo target/$target/exonum-neo4j generate-template ../../shared-config/common.toml --validators-count $1
rm -r -f ../../shared-config/*
target/$target/exonum-neo4j generate-template ../../shared-config/common.toml --validators-count $1