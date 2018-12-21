Requirements: Install screen, run*.sh requires that.

apt-get install
	cargo
	build-essential
	pkg-config
	libssl-dev
	curl
These are for being able to build the project.

cargo build // to install the thing so you can use reConf and run .sh

reConfSingle.sh // to delete all existing local DB and run all the configuration commands for 4 server setup.
runSingle.sh //Run it in screen mode (google linux screen), starts all 4 servers from single terminal.

cargo test // build and runs test-kit tests (currently 1).

Running multiple using reConf and run has to be adjusted for also using different neo4j.toml files and ports.
Propably not a good idea anymore.