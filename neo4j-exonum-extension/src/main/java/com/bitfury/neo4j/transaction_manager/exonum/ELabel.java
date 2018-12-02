package com.bitfury.neo4j.transaction_manager.exonum;

public class ELabel {

    private String nodeUUID;
    private String name;

    public ELabel(String nodeUUID, String name) {
        this.nodeUUID = nodeUUID;
        this.name = name;
    }

    public String getNodeUUID() {
        return nodeUUID;
    }

    public String getName() {
        return name;
    }



}
