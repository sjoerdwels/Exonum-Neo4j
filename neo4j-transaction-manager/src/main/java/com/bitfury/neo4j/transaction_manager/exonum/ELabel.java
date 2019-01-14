package com.bitfury.neo4j.transaction_manager.exonum;

import java.util.Comparator;

public class ELabel implements Comparable<ELabel> {

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

    @Override
    public int compareTo(ELabel o) {
        return Comparator.comparing(ELabel::getNodeUUID)
                .thenComparing(ELabel::getName)
                .compare(this, o);
    }
}
