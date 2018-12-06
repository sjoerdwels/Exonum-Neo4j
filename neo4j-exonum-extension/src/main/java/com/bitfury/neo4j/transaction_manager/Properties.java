package com.bitfury.neo4j.transaction_manager;

public class Properties {

    public static final String UUID = "uuid";

    private Properties() {
    }

    public static final String concatUUID(String prefix, int suffix) {
        return prefix + "_" + suffix;
    }
}
