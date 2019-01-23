::Node 1
docker run -t -d --name node1 -p 8201:8200 -p 7471:7474 -p 7681:7687 -p 3001:3005 -v TestVolume:/shared-config stable5 
docker exec node1 neo4j start			
docker exec node1 rm -r /shared-config/*
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node1 chmod +x genCommonConfigTesting.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node1 ./genCommonConfigTesting.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node1 chmod +x reConfTestNode1.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node1 ./reConfTestNode1.sh

::Node 2
docker run -t -d --name node2 -p 8202:8200 -p 7472:7474 -p 7682:7687 -v TestVolume:/shared-config stable5
docker exec node2 neo4j start	
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node2 chmod +x reConfTestNode2.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node2  ./reConfTestNode2.sh

::Node 3
docker run -t -d --name node3 -p 8203:8200 -p 7473:7474 -p 7683:7687 -v TestVolume:/shared-config stable5
docker exec node3 neo4j start
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node3 chmod +x reConfTestNode3.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node3 ./reConfTestNode3.sh

::Node 4
docker run -t -d --name node4 -p 8204:8200 -p 7474:7474 -p 7684:7687 -v TestVolume:/shared-config stable5
docker exec node4 neo4j start
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node4 chmod +x reConfTestNode4.sh	
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node4 ./reConfTestNode4.sh

docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node1 chmod +x finalizeTesting.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node1 ./finalizeTesting.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node2 chmod +x finalizeTesting.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node2 ./finalizeTesting.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node3 chmod +x finalizeTesting.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node3 ./finalizeTesting.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node4 chmod +x finalizeTesting.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node4 ./finalizeTesting.sh

docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node1 chmod +x runTestNode1.sh
docker exec -d -w /Exonum-Neo4j/exonum-neo4j-service/ node1 ./runTestNode1.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node2 chmod +x runTestNode2.sh
docker exec -d -w /Exonum-Neo4j/exonum-neo4j-service/ node2 ./runTestNode2.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node3 chmod +x runTestNode3.sh
docker exec -d -w /Exonum-Neo4j/exonum-neo4j-service/ node3 ./runTestNode3.sh
docker exec -w /Exonum-Neo4j/exonum-neo4j-service/ node4 chmod +x runTestNode4.sh
docker exec -d -w /Exonum-Neo4j/exonum-neo4j-service/ node4 ./runTestNode4.sh

::Setup frontend node 1
docker exec -d -w /Exonum-Neo4j/frontend/ node1 printf 'EXONUM_PRIVATE_KEY=' >> .env
docker exec -d -w /Exonum-Neo4j/frontend/ node1 grep -Po 'service_secret_key = "\K[^"]*' ../../shared-config/sec_1.toml >> .env
docker exec -d -w /Exonum-Neo4j/frontend/ node1 printf 'EXONUM_PUBLIC_KEY=' >> .env
docker exec -d -w /Exonum-Neo4j/frontend/ node1 grep -Po 'public_key = "\K[^"]*' ../../shared-config/sec_1.toml >> .env
docker exec -w /Exonum-Neo4j/frontend/ node1 sed -i "s|NEO4J_BOLT_PORT=7687|NEO4J_BOLT_PORT=7681|g" .env
docker exec -w /Exonum-Neo4j/frontend/ node1 sed -i "s|EXONUM_PORT=8200|EXONUM_PORT=8201|g" .env
::docker exec -w /Exonum-Neo4j/frontend/ node1 npm install
docker exec	-w /Exonum-Neo4j/frontend/ node1 npm start

timeout /t 300