SET node_count=4

for /l %%i in (1, 1, %node_count%) do (
	docker exec node%%i neo4j restart
)

::kill all running exonum processes and remove configuration files
for /l %%i in (1, 1, %node_count%) do (
    docker exec node%%i pkill exonum-neo4j
	docker exec node%%i rm -rf /Exonum-Neo4j/backend/db%%i
	docker exec node%%i rm -f /Exonum-Neo4j/backend/node_%%i_cfg.toml
	docker exec node%%i rm -f /Exonum-Neo4j/backend/sec_%%i.toml
)
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