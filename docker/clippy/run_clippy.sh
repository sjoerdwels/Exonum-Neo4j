#!/usr/bin/env bash

image_name="clippy"
#docker build --tag clippy .

docker run -t -d -i --name clippy $image_name

docker exec -w /Exonum-Neo4j/backend clippy git pull

docker attach -w /Exonum-Neo4j/backend clippy

#docker rm clippy

