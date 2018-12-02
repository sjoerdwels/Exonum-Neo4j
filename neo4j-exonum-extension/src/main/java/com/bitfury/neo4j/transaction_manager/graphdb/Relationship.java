package com.bitfury.neo4j.transaction_manager.graphdb;

public class Relationship {

    private String UUID;
    private String type;
    private String startNodeUUID;
    private String endNodeUUID;


    public Relationship(String UUID, String type, String startNodeUUID, String endNodeUUID) {
        this.UUID = UUID;
        this.type = type;
        this.startNodeUUID = startNodeUUID;
        this.endNodeUUID = endNodeUUID;
    }

    public String getUUID() {
        return this.UUID;
    }

    public String getType() {
        return this.type;
    }

    public String getStartNodeUUID() {
        return this.startNodeUUID;
    }

    public String getEndNodeUUID() {
        return this.endNodeUUID;
    }

}
