use std::fs::File;
use std::io::prelude::*;
use toml;


/// Attempt to load and parse the config file into our Config struct.
/// If a file cannot be found, return a default Config.
/// If we find a file but cannot parse it, panic
pub fn parse_port() -> std::io::Result<u16>{
    let path = "neo4j.toml".to_string();
    let mut config_toml = String::new();

    let mut file = File::open(&path)?;

    file.read_to_string(&mut config_toml)
            .unwrap_or_else(|err| panic!("Error while reading config: [{}]", err));

    let toml = config_toml.as_str().parse::<toml::Value>().unwrap();

    let port_str : String = toml["info"]["port"].to_string();
    let port = port_str.parse::<u16>().unwrap();

    Ok(port)
}
