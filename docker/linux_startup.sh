#!/usr/bin/env bash

image_name="stable2"
volume_name="TestVolume"
node_count=4
api_port=8200
neo4j_front_port=7470
bolt_port=7680
frontend_port=3000



for i in $(seq 1 $((node_count)))
do
    echo docker run -t -d -i --name node$i -p 820$i:8200 -p 747$i:7474 -p 768$i:7687 -p 300$i:3005 -v $volume_name:/shared-config $image_name

    docker run -t -d -i --name node$i -p 820$i:8200 -p 747$i:7474 -p 768$i:7687 -p 300$i:3005 -v $volume_name:/shared-config $image_name
    docker exec -w /Exonum-Neo4j/ node$i git pull
    docker exec node$i neo4j start
done

docker exec node1 rm -r /shared-config/*
docker exec -w /Exonum-Neo4j/backend/ node1 chmod +x genCommonConfigTesting.sh
docker exec -w /Exonum-Neo4j/backend/ node1 ./genCommonConfigTesting.sh $node_count

for i in $(seq 1 $((node_count)))
do
    docker exec -w /Exonum-Neo4j/backend/ node$i chmod +x reConfTestNode.sh
    docker exec -w /Exonum-Neo4j/backend/ node$i ./reConfTestNode.sh $i
done

for i in $(seq 1 $((node_count)))
do
    docker exec -w /Exonum-Neo4j/backend/ node$i chmod +x finalizeTesting.sh
    docker exec -w /Exonum-Neo4j/backend/ node$i ./finalizeTesting.sh $i $node_count
done

for i in $(seq 1 $((node_count)))
do
    docker exec -w /Exonum-Neo4j/backend/ node$i chmod +x runTestNode.sh
    docker exec -d -w /Exonum-Neo4j/backend/ node$i ./runTestNode.sh $i
done


::Setup frontend node 1
docker exec -d -w /Exonum-Neo4j/frontend/ node1 printf 'EXONUM_PRIVATE_KEY=' >> .env
docker exec -d -w /Exonum-Neo4j/frontend/ node1 grep -Po 'service_secret_key = "\K[^"]*' ../../shared-config/sec_1.toml >> .env
docker exec -d -w /Exonum-Neo4j/frontend/ node1 printf 'EXONUM_PUBLIC_KEY=' >> .env
docker exec -d -w /Exonum-Neo4j/frontend/ node1 grep -Po 'public_key = "\K[^"]*' ../../shared-config/sec_1.toml >> .env
docker exec -w /Exonum-Neo4j/frontend/ node1 sed -i "s|NEO4J_BOLT_PORT=7687|NEO4J_BOLT_PORT=7681|g" .env
docker exec -w /Exonum-Neo4j/frontend/ node1 sed -i "s|EXONUM_PORT=8200|EXONUM_PORT=8201|g" .env
::docker exec -w /Exonum-Neo4j/frontend/ node1 npm install
docker exec	-w /Exonum-Neo4j/frontend/ node1 npm start
