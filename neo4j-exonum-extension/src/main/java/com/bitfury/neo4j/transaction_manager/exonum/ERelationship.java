package com.bitfury.neo4j.transaction_manager.exonum;

public class ERelationship {

    private String UUID;
    private String type;
    private String startNodeUUID;
    private String endNodeUUID;


    public ERelationship(String type, String startNodeUUID, String endNodeUUID) {
        this.type = type;
        this.startNodeUUID = startNodeUUID;
        this.endNodeUUID = endNodeUUID;
    }

    public void setUUID(String UUID){ this.UUID = UUID}

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
