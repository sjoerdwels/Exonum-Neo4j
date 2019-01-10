package com.bitfury.neo4j.transaction_manager;

public class Properties {

    public static final String UUID = "uuid";


    public static final String GRPC_KEY_PORT = "transaction_manager.grpc.port";
    public static final int GRPC_DEFAULT_PORT = 9994;

    private Properties() {
    }

    public static final String concatUUID(String prefix, int suffix) {
        return prefix + "_" + suffix;
    }
}
