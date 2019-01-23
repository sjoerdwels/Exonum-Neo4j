printf 'EXONUM_PRIVATE_KEY=' >> .env
grep -Po 'service_secret_key = "\K[^"]*' ../../shared-config/sec_1.toml >> .env
printf 'EXONUM_PUBLIC_KEY=' >> .env
grep -Po 'public_key = "\K[^"]*' ../../shared-config/sec_1.toml >> .env
#sed -i "s|NEO4J_BOLT_PORT=7687|NEO4J_BOLT_PORT=7681|g" .env
#sed -i "s|EXONUM_PORT=8200|EXONUM_PORT=8201|g" .env