extern crate exonum_build;
extern crate protoc_rust_grpc;

use exonum_build::{get_exonum_protobuf_files_path, protobuf_generate};
use std::{env, fs::OpenOptions, io::Write, path::PathBuf};

fn main() {
    // v0.10.0
    // todo: enable when exonum is updated to v0.10.0
    //    let exonum_protos = get_exonum_protobuf_files_path();
    //    protobuf_generate(
    //        "../proto",
    //        &["../proto", &exonum_protos],
    //        "protobuf_mod.rs",
    //    );

    let mod_file = "protobuf_mod.rs";

    // Generate protobuff messages
    protobuf_generate("../proto", &["../proto"], &mod_file);

    // Generate gRPC services
    let out_dir: PathBuf = env::var("OUT_DIR")
        .map(PathBuf::from)
        .expect("Unable to get OUT_DIR");

    protoc_rust_grpc::run(protoc_rust_grpc::Args {
        out_dir: out_dir
            .to_str()
            .expect("Out dir name is not convertible to &str"),
        includes: &["../proto"],
        input: &["../proto/transaction_manager.proto"],
        rust_protobuf: false,
        ..Default::default()
    })
    .expect("protoc-rust-grpc");

    // Add gRPC service to mod file
    let dest_path: PathBuf = out_dir.join(mod_file);

    println!("{}", dest_path.display());

    let mut file = OpenOptions::new()
        .append(true)
        .open(dest_path)
        .expect("Unable to open mode file");

    file.write(b"pub mod transaction_manager_grpc;")
        .expect("Unable to write data to mod file");
}
