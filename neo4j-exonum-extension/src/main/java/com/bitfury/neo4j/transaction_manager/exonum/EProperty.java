package com.bitfury.neo4j.transaction_manager.exonum;

public class EProperty {

    private String UUID;
    private String key;
    private String previous_value;
    private String value;

    public EProperty(String UUID, String key, String value) {
        this.UUID = UUID;
        this.key = key;
        this.value = value;
        this.previous_value = null;
    }

    public EProperty(String UUID, String key, String previous_value, String value) {
        this.UUID = UUID;
        this.key = key;
        this.previous_value = previous_value;
        this.value = value;
    }

    public String getUUID() {
        return UUID;
    }

    public String getKey() {
        return key;
    }

    public String getPreviousValue() {
        return previous_value;
    }

    public String getValue() {
        return value;
    }
}