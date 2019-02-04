SET node_count=4

.\startup.bat

::Test1
timout 60

newman run simple_suc_transaction.postman_collection.json -e exonum_neo4j.postman_environment.json

.\reset.bat

::Test2
timeout 60

newman run simple_fail_transaction.postman_collection.json -e exonum_neo4j.postman_environment.json

.\reset.bat

::Test3
timeout 60

newman run complex_modify_suc_transaction.postman_collection.json -e exonum_neo4j.postman_environment.json

.\reset.bat

::Test4
timeout 60

newman run multiquery_suc_transaction.postman_collection.json -e exonum_neo4j.postman_environment.json

.\reset.bat

::Test5
timeout 60

newman run multiquery_fail_transaction.postman_collection.json -e exonum_neo4j.postman_environment.json

.\reset.bat

::Turn a node off (2/3 of nodes still work) and run the same tests
docker stop node4


::Test1
timout 60

newman run simple_suc_transaction.postman_collection.json -e exonum_neo4j.postman_environment.json

.\reset.bat

::Test2
timeout 60

newman run simple_fail_transaction.postman_collection.json -e exonum_neo4j.postman_environment.json

.\reset.bat

::Test3
timeout 60

newman run complex_modify_suc_transaction.postman_collection.json -e exonum_neo4j.postman_environment.json

.\reset.bat

::Test4
timeout 60

newman run multiquery_suc_transaction.postman_collection.json -e exonum_neo4j.postman_environment.json

.\reset.bat

::Test5
timeout 60

newman run multiquery_fail_transaction.postman_collection.json -e exonum_neo4j.postman_environment.json



::stop and remove all nodes
for /l %%i in (1, 1, %node_count%) do (
    docker rm -f node%%i
)


