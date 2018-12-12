package com.bitfury.neo4j.transaction_manager.exonum;

import org.neo4j.graphdb.event.TransactionData;

public interface EUUID {
    String getUUID(TransactionData transactionData, long id);
}
