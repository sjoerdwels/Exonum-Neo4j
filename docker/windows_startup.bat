SET image_name=stable6
SET volume_name=TestVolume
SET node_count=4
SET api_port=8200
SET neo4j_front_port=7470
SET bolt_port=7680
SET frontend_port=3000

for /l %%i in (1, 1, %node_count%) do (
    docker run -t -d -i --name node%%i -p 820%%i:8200 -p 747%%i:7474 -p 768%%i:7687 -p 300%%i:3005 -v %volume_name%:/shared-config %image_name%
	docker exec -w /Exonum-Neo4j/ node%%i git pull
    docker exec node%%i neo4j start
)

::cleaning shared_config folder and making common conf
docker exec -w /Exonum-Neo4j/backend/ node1 ./genCommonConfigTesting.sh %node_count%

::Generating conf for each node
for /l %%i in (1, 1, %node_count%) do (
    docker exec -w /Exonum-Neo4j/backend/ node%%i ./reConfTestNode.sh %%i
)

::Finalizing conf for each node
for /l %%i in (1, 1, %node_count%) do (
    docker exec -w /Exonum-Neo4j/backend/ node%%i ./finalizeTesting.sh %%i %node_count%
)

::Run all the nodes
for /l %%i in (1, 1, %node_count%) do (
    docker exec -d -w /Exonum-Neo4j/backend/ node%%i ./runTestNode.sh %%i
)

::echo "Setup finisehd"
::echo "Navigate to 198.168.99.100:3001 to access the demo application"
pause