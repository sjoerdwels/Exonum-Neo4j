SET image_name=exonum_neo4j
SET volume_name=ExonumConfVolume
SET node_count=4
SET api_port=8200
SET neo4j_front_port=7470
SET bolt_port=7680
SET frontend_port=3000
SET target="release"

for /l %%i in (1, 1, %node_count%) do (
    docker run -t -d -i --name node%%i -p 820%%i:8200 -p 747%%i:7474 -p 768%%i:7687 -p 300%%i:3005 -v %volume_name%:/shared-config %image_name%
    docker exec -w /Exonum-Neo4j/ node%%i git pull
	docker exec node%%i neo4j-admin set-initial-password exonumNeo4j
	docker exec node%%i neo4j start
)

::cleaning shared_config folder and making common conf
docker exec -w /Exonum-Neo4j/backend/ node1 ./genCommonConfigTesting.sh %node_count% %target%

::Generating conf for each node
for /l %%i in (1, 1, %node_count%) do (
    docker exec -w /Exonum-Neo4j/backend/ node%%i ./reConfTestNode.sh %%i %target%
)

::Finalizing conf for each node
for /l %%i in (1, 1, %node_count%) do (
    docker exec -w /Exonum-Neo4j/backend/ node%%i ./finalizeTesting.sh %%i %node_count% %target%
)

::Run all the nodes
for /l %%i in (1, 1, %node_count%) do (
    docker exec -d -w /Exonum-Neo4j/backend/ node%%i ./runTestNode.sh %%i %target%
)

::Setup frontend
for /l %%i in (1, 1, %node_count%) do (
    docker exec -w /Exonum-Neo4j/frontend/ node%%i ./genEnv.sh %%i 192.168.99.100
    docker exec -d -w /Exonum-Neo4j/frontend/ node%%i npm start
)

echo "Setup finisehd"
echo "Navigate to 198.168.99.100:3001 to access the demo application"
pause