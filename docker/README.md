install docker

Move to the folder where Dockerfile resides

docker build --tag exonum_neo4j .

That makes an image. It takes a while.


docker images .... list images
docker rmi <image_name> ... <image_name_n> ... delete image(s)
docker ps -a ... shows all running containers
docker rm <container_name> ...<container_name_n>... deletes container(s)

To make a shared directory:

docker volume create --name ExonumConfVolume

If you use different name for volume or image, then you need to change the values also in <OP>_startup script.

For windows execute windows_startup.bat
For linux execute linux_startup.sh

To access the frontends for all the nodes, use:
Windows:
192.168.99.100:3001-3004
Linux:
localhost:3001-3004 (depending on the node)