package com.bitfury.neo4j.transaction_manager;

import java.io.IOException;
import java.util.List;

import com.google.protobuf.Message;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.logging.Log;

public class TransactionManager extends TransactionManagerGrpc.TransactionManagerImplBase {

    private static GraphDatabaseService db;
    private static LogService logService;
    private static Config config;

    private Log log;
    private Server gRPCserver;
    private TransactionManagerEventHandler eventHandler;

    private static final Object lock = new Object();
    private static TransactionData transactionData = null;

    private enum TransactionType {
        VERIFY,
        EXECUTE
    }

    TransactionManager(GraphDatabaseService db, Config config, LogService logService) {

        TransactionManager.db = db;
        TransactionManager.logService = logService;
        TransactionManager.config = config;
        log = logService.getUserLog(getClass());

        // Start gRPC server
        gRPCserver = ServerBuilder.forPort(9999).addService(this).build();

        // Register EventHandler
        eventHandler = new TransactionManagerEventHandler(db, this);
        db.registerTransactionEventHandler(eventHandler);

        log.info("Transaction manager started.");
    }

    public void start() {
        try {
            gRPCserver.start();
        } catch (IOException e) {
            log.error("Could not start gRPC server, shutdown database.");
            db.shutdown();
        }
    }

    public void shutdown() {
        log.info("Shutdown transaction manager");
        gRPCserver.shutdown();
    }

    public void handleTransactionData(TransactionData data) {
        transactionData = data;
        lock.notify();
    }

    @Override
    public void verifyTransaction(TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        handleTransaction(TransactionType.VERIFY, request, responseObserver);
    }

    @Override
    public void executeTransaction(TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        handleTransaction(TransactionType.EXECUTE, request, responseObserver);
    }


    private void handleTransaction(TransactionType type, TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        try {

            // Verify transaction
            if (request.getQueriesCount() == 0) {
                throw new Exception("Transaction has no insert queries.");
            }

            List<String> queries = request.getQueriesList();

            synchronized (lock) {

                TransactionResponse.Builder responseBuilder = TransactionResponse.newBuilder();

                // Create transaction
                try (Transaction tx = db.beginTx()) {

                    for (String query : queries) {

                        if (validInsertQuery(query)) {
                            db.execute(query);
                        }
                    }

                    // Rollback / commit transaction
                    switch (type) {
                        case VERIFY:
                            break;
                        case EXECUTE:
                            tx.success();
                            break;
                    }

                    // Wait for TransactionEventHandler to notify that the changes are available.
                    lock.wait();

                    // Set success status
                    responseBuilder.setResult(Status.SUCCESS);

                    // Add transactionData
                    addTransactionData(responseBuilder);

                } catch (Exception ex) {
                    // Query was not accepted by DB, response failure
                    responseBuilder.setResult(Status.FAILURE);

                    log.info("Transaction was not accepted by database");
                }

                // Send response
                responseObserver.onNext(responseBuilder.build());

                // Close observer
                responseObserver.onCompleted();

                log.info("Transaction with " + request.getQueriesCount() + " queries successful " + type.name().toLowerCase() + ".");
            }
        } catch (Exception ex) {
            log.error("Error handling transaction: " + ex.getMessage());
            responseObserver.onError(ex);
        }
    }

    private void addTransactionData(TransactionResponse.Builder responseBuilder) {
        // todo add all transcation data in the new protocol format.
    }

    private boolean validInsertQuery(String query) throws Exception {
        // todo check if Query is insert query and does not get results.
        return true;
    }
}
