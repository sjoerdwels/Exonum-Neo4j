package com.bitfury.neo4j.transaction_manager;

import org.neo4j.graphdb.Relationship;

import java.util.Comparator;

public class RelationshipComparator implements Comparator<Relationship> {

    @Override
    public int compare(Relationship a, Relationship b) {
        if (a.getId() < b.getId()) {
            return -1;
        }
        if (a.getId() > b.getId()) {
            return 1;
        }
        return 0;
    }
}