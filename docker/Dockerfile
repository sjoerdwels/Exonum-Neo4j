FROM ubuntu

ARG branch=development

#Install Git and pull project
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y git	
RUN git clone https://github.com/sjoerdwels/Exonum-Neo4j.git
RUN cd Exonum-Neo4j &&\
		git checkout $branch

#Install Rust
RUN apt-get install curl -y &&\
	curl https://sh.rustup.rs -sSf > rustup.sh &&\
	chmod 755 rustup.sh &&\
	./rustup.sh -y

#Setup Exonum
RUN apt-get update && \
	apt-get install -y build-essential \
	pkg-config \
	libssl-dev

#Install protobuf
RUN curl -OL https://github.com/protocolbuffers/protobuf/releases/download/v3.6.1/protoc-3.6.1-linux-x86_64.zip &&\
	apt-get install unzip &&\
	unzip -o protoc-3.6.1-linux-x86_64.zip -d /usr/local bin/protoc &&\
	rm -f protoc-3.6.1-linux-x86_64.zip

#Start Exonum service
RUN cd Exonum-Neo4j/exonum-neo4j-service &&\
		. ~/.cargo/env &&\
		cargo build
		
#Install Neo4j
RUN apt-get install software-properties-common -y
RUN apt-get update &&\
	apt install openjdk-8-jdk -y 
RUN apt-get install wget &&\
	wget -O - https://debian.neo4j.org/neotechnology.gpg.key | apt-key add - &&\
	echo 'deb https://debian.neo4j.org/repo stable/' | tee -a /etc/apt/sources.list.d/neo4j.list &&\
	apt-get update -y	&&\
	apt-get install neo4j=1:3.5.1 -y &&\
	sed -i "s|#dbms.connectors.default_listen_address=0.0.0.0|dbms.connectors.default_listen_address=0.0.0.0|g" /etc/neo4j/neo4j.conf #In order to access neo4j from host
	
#Install maven
RUN apt-get install maven -y

# Package the Neo4j extention
RUN cd Exonum-Neo4j/neo4j-transaction-manager &&\ 
		mvn package
RUN cp Exonum-Neo4j/neo4j-transaction-manager/target/transaction-manager-0.1.0-shaded.jar /var/lib/neo4j/plugins/


#Setup frontend
RUN apt-get update -y &&\
	apt-get install -y nodejs \
	npm
RUN npm install npm@latest -g &&\
	/bin/bash -c "hash -d npm"		
RUN cd /Exonum-Neo4j/frontend/ &&\
	npm install

#Add nano as useful program for debugging
RUN apt-get install nano


EXPOSE 8200 8080 7474 7473
