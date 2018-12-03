package com.bitfury.neo4j.transaction_manager;

import java.io.IOException;
import java.util.List;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.event.TransactionData;

import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.api.txstate.TransactionState;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.logging.Log;

import com.bitfury.neo4j.transaction_manager.exonum.*;

public class TransactionManager extends TransactionManagerGrpc.TransactionManagerImplBase {

    private static GraphDatabaseService db;
    private static LogService logService;
    private static Config config;

    private Log log;
    private Server gRPCServer;
    private TransactionManagerEventHandler eventHandler;

    ThreadLocal<TransactionStateMachine> TmData = new ThreadLocal<>();

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
        handleTransaction(TransactionStateMachine.TransactionType.VERIFY, request, responseObserver);
    }

    @Override
    public void executeTransaction(TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        log.info("method=executeTransaction request=" + request.toString());
        handleTransaction(TransactionStateMachine.TransactionType.EXECUTE, request, responseObserver);
    }

    /**
     * Handle a new transaction by executing the provided queries and return the observer whether the transaction was successful.
     * Uses a thread local TmData object to store all related data to the gRPC request,
     * as the TransactionEventHandler has no reference to the gRPC executor instance.
     *
     * @param type             The gRPC method type
     * @param request          The request message
     * @param responseObserver The responseObserver
     */
    private void handleTransaction(TransactionStateMachine.TransactionType type, TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {

        log.debug("method=handleTransaction type=" + type.name() + "totalQueries=" + request.getQueriesCount()
                + " threadID=" + Thread.currentThread().getId());

        try {

            // TODO check if UUID is set

            TmData.set(new TransactionStateMachine(type, request.getUUIDPrefix()));

            if (request.getQueriesCount() == 0) {
                throw new Exception("Transaction has no insert queries.");
            }

            List<String> queries = request.getQueriesList();

            try (Transaction tx = db.beginTx()) {

                for (String query : queries) {
                    db.execute(query);
                }

                TmData.get().readyToCommit();

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
     * Called by TransactionEventHandler after a Neo4j transaction has successfully been committed.
     * Processes the transaction data whether needed.
     *
     * @param transactionData The changes that were committed in this transaction.
     */
    public void afterCommit(TransactionData transactionData) {

        switch(TmData.get().getStatus()) {
            case READY_TO_COMMIT:

                TmData.get().committed();

                assignUUIDS( transactionData );

                TmData.get().assignedUUIDs();

                storeModifications( transactionData );

                TmData.get().storedModifications();

                break;
            default:
                log.debug("method=afterCommit, TmData.status=" + TmData.get().getStatus() + ", transaction data not  processed");
        }

    }

    /**
     * Called after transaction has been rolled back because committing the transaction failed.
     * Could be caused by external Transaction Event Handlers
     *
     * @param transactionData The changes that were attempted to be committed in this transaction.
     */
    public void afterRollback(TransactionData transactionData) {
        TmData.get().rolledback();
    }

    /**
     *
     * @param transactionData
     */
    private void assignUUIDS(TransactionData transactionData) {

        TransactionStateMachine tsm = TmData.get();

        int uuid_id = 0;

        // todo assign UUIDS to all new created relationships & nodes

        for (Node node : transactionData.createdNodes()) {

        }

        TmData.set(tsm);
    }

    /**
     *
     * @param transactionData
     */
    private void storeModifications(TransactionData transactionData) {

        for (Node node : transactionData.createdNodes()) {

        }
    }

}
