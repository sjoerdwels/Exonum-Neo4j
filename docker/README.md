install docker

Move to the folder where Dockerfile resides

docker build --tag <image_name> .

That makes an image. It takes a while.


docker images .... list images
docker rmi <image_name> ... <image_name_n> ... delete image(s)
docker ps -a ... shows all running containers
docker rm <container_name> ...<container_name_n>... deletes container(s)

To make a shared directory:

docker volume create --name <volume_name> ... currently set to testVolume. But we need to change it as a variable in a script.

The configuration for frontend. Is localhost:exposed_port for neo4j. And 172.17...:8200 for exonum.
