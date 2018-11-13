#
# Makefile
# @author Evgeny Ukhanov <mrlsd@ya.ru>
#

.PHONY: check, clean, run, build, release, install, test, doc
default: check

test:
	@echo Run tests...
	@RUSTFLAGS="-D warnings" cargo test lifecycle_reveal -- --nocapture
#	@RUSTFLAGS="-D warnings" cargo test --all -- --nocapture
	@echo Done.

check:
#	@cargo check --all
	@RUSTFLAGS="-D warnings" cargo check --all

clean:
	@rm -rf config/db*/

run:
	@echo "Build..."
	@RUSTFLAGS="-D warnings" cargo build

	@echo "Run nodes..."
	@target/debug/exonum-neo4j run --node-config config/node_1_cfg.toml --db-path config/db1 --public-api-address 0.0.0.0:8200
#	@target/debug/exonum-neo4j run --node-config config/node_2_cfg.toml --db-path config/db2 --public-api-address 0.0.0.0:8201&
#	@target/debug/exonum-neo4j run --node-config config/node_3_cfg.toml --db-path config/db3 --public-api-address 0.0.0.0:8202&
#	@target/debug/exonum-neo4j run --node-config config/node_4_cfg.toml --db-path config/db4 --public-api-address 0.0.0.0:8203&

create-config:
	@echo "Create directory..."
	@rm -rf config/ || true
	@mkdir config || true

	@echo "Build..."
	@cargo build

	@echo "Generate template..."
	@target/debug/exonum-neo4j generate-template config/common.toml --validators-count 1

	@echo "Generate public and secrets keys for each node..."
	@target/debug/exonum-neo4j generate-config config/common.toml  config/pub_1.toml config/sec_1.toml --peer-address 127.0.0.1:6331
#	@target/debug/exonum-neo4j generate-config config/common.toml  config/pub_2.toml config/sec_2.toml --peer-address 127.0.0.1:6332
#	@target/debug/exonum-neo4j generate-config config/common.toml  config/pub_3.toml config/sec_3.toml --peer-address 127.0.0.1:6333
#	@target/debug/exonum-neo4j generate-config config/common.toml  config/pub_4.toml config/sec_4.toml --peer-address 127.0.0.1:6334

	@echo "Finalize configs..."
	@target/debug/exonum-neo4j finalize --public-api-address 0.0.0.0:8200 --private-api-address 0.0.0.0:8091 config/sec_1.toml config/node_1_cfg.toml --public-configs config/pub_1.toml
#	@target/debug/exonum-neo4j finalize --public-api-address 0.0.0.0:8201 --private-api-address 0.0.0.0:8092 config/sec_2.toml config/node_2_cfg.toml --public-configs config/pub_1.toml config/pub_2.toml config/pub_3.toml config/pub_4.toml
#	@target/debug/exonum-neo4j finalize --public-api-address 0.0.0.0:8202 --private-api-address 0.0.0.0:8093 config/sec_3.toml config/node_3_cfg.toml --public-configs config/pub_1.toml config/pub_2.toml config/pub_3.toml config/pub_4.toml
#	@target/debug/exonum-neo4j finalize --public-api-address 0.0.0.0:8203 --private-api-address 0.0.0.0:8094 config/sec_4.toml config/node_4_cfg.toml --public-configs config/pub_1.toml config/pub_2.toml config/pub_3.toml config/pub_4.toml

build:
	@echo Build debug version...
	@RUSTFLAGS="-D warnings" cargo build --all
	@echo Done.

release:
	@echo Build release version...
	@cargo build --release
	@echo Done.

install:
	@echo Install new Rust stable varsion...
	@curl -s https://static.rust-lang.org/rustup.sh | sudo sh
	@echo Installed:
	@rustc --version
	@echo Done.

build-doc:
	@echo Build documentation...
	@cargo doc --no-deps
	@echo Done.
