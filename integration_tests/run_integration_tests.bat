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

for /l %%i in (1, 1, %node_count%) do (
    docker rm -f node%%i
)


