SET node_count=4

for /l %%i in (1, 1, %node_count%) do (
	docker exec node%%i neo4j restart
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