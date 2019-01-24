#!/bin/bash

node=$1
ip=$2

echo 'WEBSERVER_PORT=3005' >> .env
echo "NEO4J_BOLT_ADDRESS=$ip" >> .env
echo "NEO4J_BOLT_PORT=768$node" >> .env
echo "NEO4J_USERNAME=neo4j" >> .env
echo "NEO4J_PASSWORD=exonumNeo4j" >> .env
echo "EXONUM_ADDRESS=172.17.0.$((node+1))" >> .env
echo "EXONUM_PORT=8200" >> .env

printf 'EXONUM_PRIVATE_KEY=' >> .env
grep -Po 'service_secret_key = "\K[^"]*' ../backend/sec_$node.toml >> .env
printf 'EXONUM_PUBLIC_KEY=' >> .env
grep -Po 'public_key = "\K[^"]*' ../backend/sec_$node.toml >> .env