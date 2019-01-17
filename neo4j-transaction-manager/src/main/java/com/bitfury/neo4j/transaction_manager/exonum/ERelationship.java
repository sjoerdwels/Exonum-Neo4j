package com.bitfury.neo4j.transaction_manager.exonum;

import java.util.Comparator;

public class ERelationship implements Comparable<ERelationship> {

    private String UUID;
    private String type;
    private String startNodeUUID;
    private String endNodeUUID;

    public ERelationship(String UUID, String type, String startNodeUUID, String endNodeUUID) {
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

    @Override
    public int compareTo(ERelationship o) {
        return Comparator.comparing(ERelationship::getUUID)
                .thenComparing(ERelationship::getType)
                .thenComparing(ERelationship::getStartNodeUUID)
                .thenComparing(ERelationship::getEndNodeUUID)
                .compare(this, o);
    }
}
