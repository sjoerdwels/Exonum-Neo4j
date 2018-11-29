package com.bitfury.neo4j.transaction_manager;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;

public class TransactionManagerEventHandler implements TransactionEventHandler {


    private static GraphDatabaseService db;
    private static TransactionManager manager;

    public TransactionManagerEventHandler(GraphDatabaseService graphDatabaseService, TransactionManager transactionManager) {
        db = graphDatabaseService;
        manager = transactionManager;
    }

    @Override
    public Object beforeCommit(TransactionData transactionData) {
        return null;
    }

    @Override
    public void afterCommit(TransactionData transactionData, Object o) {
        manager.handleTransactionData( transactionData );
    }

    @Override
    public void afterRollback(TransactionData transactionData, Object o) {
        manager.handleTransactionData( transactionData );
    }

}
