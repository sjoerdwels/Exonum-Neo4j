package com.bitfury.neo4j.transaction_manager;

import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.logging.Log;

public class TransactionManagerEventHandler implements TransactionEventHandler {

    private static TransactionManager manager;
    private Log userLog;

    public TransactionManagerEventHandler(TransactionManager transactionManager, Log userLog) {
        manager = transactionManager;
        this.userLog = userLog;
    }

    @Override
    public Object beforeCommit(TransactionData transactionData) throws Exception {
        userLog.debug("method=beforeCommit threadID=" +Thread.currentThread().getId() );
        manager.beforeCommit(transactionData);
        return null;
    }

    @Override
    public void afterCommit(TransactionData transactionData, Object o) {
        userLog.debug("method=afterCommit threadID= " +Thread.currentThread().getId() );
        manager.afterCommit(transactionData);
    }

    @Override
    public void afterRollback(TransactionData transactionData, Object o) {
        userLog.debug("method=afterRollback threadID=" +Thread.currentThread().getId() );
        manager.afterRollback(transactionData);
    }

}
