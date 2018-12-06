package com.bitfury.neo4j.transaction_manager.exonum;

public class EProperty {

    private String UUID;
    private String key;
    private String value;

    public EProperty(String UUID, String key) {
        this.UUID = UUID;
        this.key = key;
        this.value = null;
    }

    public EProperty(String UUID, String key, String value) {
        this.UUID = UUID;
        this.key = key;
        this.value = value;
    }

    public String getUUID() {
        return UUID;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
