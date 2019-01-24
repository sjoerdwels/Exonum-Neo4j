#!/usr/bin/env bash

image_name="stable2"
volume_name="TestVolume"
node_count=4
api_port=8200
neo4j_front_port=7470
bolt_port=7680
frontend_port=3000


#Starting nodes and neo4j
for i in $(seq 1 $((node_count)))
do
    echo docker run -t -d -i --name node$i -p 820$i:8200 -p 747$i:7474 -p 768$i:7687 -p 300$i:3005 -v $volume_name:/shared-config $image_name
    docker run -t -d -i --name node$i -p 820$i:8200 -p 747$i:7474 -p 768$i:7687 -p 300$i:3005 -v $volume_name:/shared-config $image_name
    docker exec -w /Exonum-Neo4j/ node$i git pull
    docker exec node$i neo4j start
done

#cleaning shared_config folder and making common conf
docker exec -w /Exonum-Neo4j/backend/ node1 ./genCommonConfigTesting.sh $node_count

#Generating conf for each node
for i in $(seq 1 $((node_count)))
do
    docker exec -w /Exonum-Neo4j/backend/ node$i ./reConfTestNode.sh $i
done

#Finalizing conf for each node
for i in $(seq 1 $((node_count)))
do
    docker exec -w /Exonum-Neo4j/backend/ node$i ./finalizeTesting.sh $i $node_count
done

#Run all the nodes
for i in $(seq 1 $((node_count)))
do
    docker exec -d -w /Exonum-Neo4j/backend/ node$i ./runTestNode.sh $i
done


#Setup frontend
for i in $(seq 1 $((node_count)))
do
    docker exec -w /Exonum-Neo4j/frontend/ node$i ./genEnv.sh $i localhost
    docker exec -d -w /Exonum-Neo4j/frontend/ node$i npm start
done