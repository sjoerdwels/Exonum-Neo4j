package com.bitfury.neo4j.transaction_manager.exonum;

public class EError {

    public enum ErrorType {
        EMPTY_TRANSACTION,
        EMPTY_UUID_PREFIX,
        FAILED_QUERY,
        MODIFIED_UUID,
        TRANSACTION_ROLLBACK,
        RUNTIME_EXCEPTION
    }

    private ErrorType type;
    private String message;
    private EFailedQuery failedQuery;

    public EError(ErrorType type,  String message) {
        this.type = type;
        this.message  = message;
        this.failedQuery = null;
    }

    public EError(ErrorType type,  String message, EFailedQuery failedQuery) {
        this.type = type;
        this.message  = message;
        this.failedQuery = failedQuery;
    }

    public String getMessage() {
        return message;
    }

    public ErrorType getType() {
        return type;
    }

    public  EFailedQuery  getFailedQuery() {
        return failedQuery;
    }

    public boolean  hasFailedQuery()  {
        return  failedQuery != null;
    }
}
