# Docker Demo Build

With the dockerfile a fully functional node running our solution can be created. The created image includes:
- A local Neo4j database
- The exonum service
- Webserver running the Neo4j Movie example 

## Requirements
-  Docker

## Installation and run

1. Open the docker directory inside the repository.

2. Run the following command to generate the docker image.

    ``` bash
    docker build --tag exonum_neo4j .
    ```

3. Create a shared volume to store the config files which are used by the docker containers. 

    ```bash
    docker volume create --name ExonumConfVolume
    ```

    Each docker container needs to have the public config of every other container to know their exonum public keys as 
    well as the common config file for exonum. For simplicity, we created a shared volume to only generate and store these files
    in one place.
    
4. If you are on a Unix machine, run the unix_startup script
    ```bash
    ./unix_startup.sh
    ```` 
   Similarly, execute the windows_startup.bat script on Windows.
   ```bash
   .\windows_startup.bat
   ````
    
    Possibly, you first have to make the script executable
    ```bash
    chmod +x unix_startup.sh
    ```

    The script will start 4 nodes. For each node it will
    - generate public / private keys
    - finalize the configs
    - generate common configs
    
    These configs will be copied to  the previous created 'ExonumConfVolume'.
    
5.  For each node, you can access:
    - neo4j : 747[1]
    - exonum : 820[1]
    - frontend : 300[1]
    on localhost with [1], the number of the node.
    
    For example, [localhost:3001](http://localhost:3001) will open the frontend of Node 1.


## Regenerate docker image
To regenerate the created docker image, simply open the docker directory in the repository and execute
```bash
docker build --tag exonum_neo4j . --no-cache

``` 
The __--no-cache__ parameter will ensure that a clean image will be created, such that the lasted version of the repository will be cloned.



## Delete created images
To delete the created docker image:

```bash
docker rmi exonum_neo4j
```

To delete the created docker, first stop the containers:
```bash
docker stop node1 node2 node3 node4
```

and subsequently delete the containers:

```bash
docker rm node1 node2 node3 node4
```