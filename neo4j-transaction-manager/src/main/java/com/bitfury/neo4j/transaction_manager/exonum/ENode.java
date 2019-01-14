package com.bitfury.neo4j.transaction_manager.exonum;

import java.util.Comparator;

public class ENode implements Comparable<ENode> {

    private String UUID;

    public ENode(String UUID) {
        this.UUID = UUID;
    }

    public String getUUID() {
        return this.UUID;
    }

    @Override
    public int compareTo(ENode o) {
        return Comparator.comparing(ENode::getUUID)
                .compare(this, o);
    }
}
