package com.bitfury.neo4j.transaction_manager.graphdb;

public class Label {

    private String nodeUUID;
    private String name;

    public Label(String nodeUUID, String name) {
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
