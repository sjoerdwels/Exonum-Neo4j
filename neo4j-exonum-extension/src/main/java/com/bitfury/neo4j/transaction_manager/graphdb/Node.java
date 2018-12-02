package com.bitfury.neo4j.transaction_manager.graphdb;

public class Node {

    private String UUID;

    public Node (String UUID) {
        this.UUID = UUID;
    }

    public String getUUID() {
        return this.UUID;
    }

}
