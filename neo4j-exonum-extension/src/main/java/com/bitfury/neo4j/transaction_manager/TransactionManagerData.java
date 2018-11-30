package com.bitfury.neo4j.transaction_manager;

import java.lang.reflect.Type;

public class TransactionManagerData {

    public enum TransactionType {
        VERIFY,
        EXECUTE
    }

    private TransactionType     transactionType;
    private boolean             isCommitted = false;
    private boolean             isRolledback = false;
    private boolean             failure = false;

    public TransactionManagerData( TransactionType type ) {
        transactionType = type;
    }

    public void isCommitted() {
        isCommitted = true;
    }

    public void isRolledback() {
        isRolledback = true;
    }

    public void failure() {
        failure = true;
    }

    public TransactionResponse getTransactionResponse() {

        TransactionResponse.Builder builder=TransactionResponse.newBuilder().setResult(getStatus());

        return builder.build();
    }

    private Status getStatus() {

        boolean success = false;

        switch(this.transactionType) {
            case VERIFY:
                success = !failure;
                break;
            case EXECUTE:
                success = isCommitted;
                break;
        }

        return success ? Status.SUCCESS :  Status.FAILURE;
    }

}
