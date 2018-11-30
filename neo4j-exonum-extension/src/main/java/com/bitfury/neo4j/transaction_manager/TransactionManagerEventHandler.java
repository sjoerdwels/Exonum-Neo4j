package com.bitfury.neo4j.transaction_manager;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.logging.Log;

public class TransactionManagerEventHandler implements TransactionEventHandler {

    private static TransactionManager manager;
    private Log log;

    public TransactionManagerEventHandler(TransactionManager transactionManager, LogService logServiceLogService) {
        manager = transactionManager;
        log = logServiceLogService.getUserLog(getClass());
    }

    @Override
    public Object beforeCommit(TransactionData transactionData) {
        log.debug("method=beforeCommit threadID=" +Thread.currentThread().getId() );
        return null;
    }

    @Override
    public void afterCommit(TransactionData transactionData, Object o) {
        log.debug("method=afterCommit threadID= " +Thread.currentThread().getId() );
        manager.afterCommit(transactionData);
    }

    @Override
    public void afterRollback(TransactionData transactionData, Object o) {
        log.debug("method=afterRollback threadID=" +Thread.currentThread().getId() );
        manager.afterRollback(transactionData);
    }

}
