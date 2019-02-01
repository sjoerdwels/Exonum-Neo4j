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
RUN cd Exonum-Neo4j/backend &&\
		. ~/.cargo/env &&\
		cargo build
		
RUN cd Exonum-Neo4j/backend &&\
		. ~/.cargo/env &&\
		rustup update && rustup component add clippy && cargo clippy