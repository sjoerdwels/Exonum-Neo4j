package com.bitfury.neo4j.transaction_manager.exonum;

public class ENode {

    private String UUID;

    public ENode(String UUID) {
        this.UUID = UUID;
    }

    public String getUUID() {
        return this.UUID;
    }

}
