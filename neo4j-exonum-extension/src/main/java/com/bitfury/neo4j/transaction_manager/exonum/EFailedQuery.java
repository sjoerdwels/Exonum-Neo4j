package com.bitfury.neo4j.transaction_manager.exonum;

public class EFailedQuery {

    private String query;
    private String error;

    public  EFailedQuery(String query, String error) {
        this.query = query;
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public String getQuery() {
        return query;
    }
}
