package com.bitfury.neo4j.transaction_manager;

import java.io.IOException;
import java.util.List;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.event.TransactionData;

import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.logging.Log;


public class TransactionManager extends TransactionManagerGrpc.TransactionManagerImplBase {

    private static GraphDatabaseService db;
    private static LogService logService;
    private static Config config;

    private Log log;
    private Server gRPCServer;
    private TransactionManagerEventHandler eventHandler;

    ThreadLocal<TransactionManagerData> TmData = new ThreadLocal<>();

    TransactionManager(GraphDatabaseService db, Config config, LogService logService) {

        TransactionManager.db = db;
        TransactionManager.logService = logService;
        TransactionManager.config = config;
        log = logService.getUserLog(getClass());

        // Start gRPC server
        gRPCServer = ServerBuilder.forPort(9994).addService(this).build();

        // Register EventHandler
        eventHandler = new TransactionManagerEventHandler(this, logService);
        db.registerTransactionEventHandler(eventHandler);

        log.info("method=constructor gRPCPort=" + 9994); //todo correct port from config
    }

    public void start() {

        try {
            log.info("method=start");
            gRPCServer.start();
        } catch (IOException e) {
            log.error("method=start error=IOException shutdown database");
            db.shutdown();
        }
    }

    public void shutdown() {
        log.info("method=shutdown");
        gRPCServer.shutdown();
    }

    @Override
    public void verifyTransaction(TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        log.info("method=verifyTransaction request=" + request.toString());
        handleTransaction(TransactionManagerData.TransactionType.VERIFY, request, responseObserver);
    }

    @Override
    public void executeTransaction(TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        log.info("method=executeTransaction request=" + request.toString());
        handleTransaction(TransactionManagerData.TransactionType.EXECUTE, request, responseObserver);
    }

    /**
     *
     * @param type              The gRPC method type
     * @param request           The request message
     * @param responseObserver  The responseObserver
     */
    private void handleTransaction(TransactionManagerData.TransactionType type, TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {

        log.debug("method=handleTransaction type=" + type.name() + "totalQueries=" + request.getQueriesCount()
                + " threadID=" + Thread.currentThread().getId());

        TmData.set(new TransactionManagerData(type));

        try {

            if (request.getQueriesCount() == 0) {
                throw new Exception("Transaction has no insert queries.");
            }

            List<String> queries = request.getQueriesList();

            try (Transaction tx = db.beginTx()) {

                for (String query : queries) {
                    db.execute(query);
                }

                switch (type) {
                    case VERIFY:
                        tx.failure();
                        break;
                    case EXECUTE:
                        tx.success();
                        break;
                }
            } catch (Exception ex) {
                log.info("invalid query");
                TmData.get().failure();
            }

            /* TransactionEventHandler hooks are called in the close() block of the try-with-resource statement.
                At this point, the transactionData is available if provided. */

            responseObserver.onNext(TmData.get().getTransactionResponse());

            responseObserver.onCompleted();

            log.info("Transaction with " + request.getQueriesCount() + " queries successful " + type.name().toLowerCase() + ".");

        } catch (Exception ex) {
            // todo Nice error handling
            log.error("Error handling transaction: " + ex.getMessage());
            responseObserver.onError(ex);
        }

    }

    /**
     * Called after transaction has successfully been committed.
     *
     * @param transactionData The changes that were committed in this transaction.
     */
    public void afterCommit(TransactionData transactionData) {
        TmData.get().isCommitted();

        // todo add TransactionData
    }

    /**
     * Called after transaction has been rolled back because committing the transaction failed.
     * Could be caused by external Transaction Event Handlers
     *
     * @param transactionData The changes that were attempted to be committed in this transaction.
     */
    public void afterRollback(TransactionData transactionData) {
        TmData.get().isRolledback();
    }
}
