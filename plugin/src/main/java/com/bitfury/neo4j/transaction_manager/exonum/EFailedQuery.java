package com.bitfury.neo4j.transaction_manager.exonum;

public class EFailedQuery {

    private String query;
    private String error;
    private String statusCode;

    public EFailedQuery(String query, String error, String statusCode) {
        this.query = query;
        this.error = error;
        this.statusCode = statusCode;
    }

    public String getError() {
        return error;
    }

    public String getQuery() {
        return query;
    }

    public String getStatusCode() {
        return statusCode;
    }
}
