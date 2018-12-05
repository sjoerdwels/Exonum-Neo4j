package com.bitfury.neo4j.transaction_manager;

import com.bitfury.neo4j.transaction_manager.exonum.ENode;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.LabelEntry;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.logging.Log;

import java.io.IOException;
import java.util.List;

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
     * @ref https://neo4j.com/docs/java-reference/current/transactions/ 7.6.1  - Neo4j transaction in a single thread to creat
     */
    private void handleTransaction(TransactionStateMachine.TransactionType type, TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {

        log.debug("method=handleTransaction type=" + type.name() + "totalQueries=" + request.getQueriesCount()
                + " threadID=" + Thread.currentThread().getId());

        try {

            if(request.getUUIDPrefix().isEmpty()) {
                throw new Exception("ResponseRequest did not provide UUID prefix.");
            }

            TmData.set(new TransactionStateMachine(type, request.getUUIDPrefix()));

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

    public void beforeCommit(TransactionData transactionData) throws Exception {

        switch (TmData.get().getStatus()) {
            case INITIAL:

                if (hasPropertyChange(transactionData, Properties.UUID)) {
                    throw new Exception("A query tried to modify a UUID, which is not allowed.");
                }

                assignUUIDS(transactionData);

                TmData.get().readyToCommit();

                break;
            default:
                log.debug("method=afterCommit, TmData.status=" + TmData.get().getStatus() + ", transaction data not  processed");
        }
    }

    /**
     * Called by TransactionEventHandler after a Neo4j transaction has successfully been committed.
     * Processes the transaction data whether needed.
     *
     * @param transactionData The changes that were committed in this transaction.
     */
    public void afterCommit(TransactionData transactionData) {


        switch (TmData.get().getStatus()) {
            case READY_TO_COMMIT:

                TmData.get().committed();

                storeModifications(transactionData);

                TmData.get().finished();

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
     * @param transactionData
     */
    private void assignUUIDS(TransactionData transactionData) {

        String uuid = TmData.get().getUuidPrefix();
        int counter = 0;

        for (Node node : transactionData.createdNodes()) {
            node.setProperty(Properties.UUID, Properties.concatUUID(uuid, counter));
            counter++;
        }

        for (Relationship relationship : transactionData.createdRelationships()) {
            relationship.setProperty(Properties.UUID, Properties.concatUUID(uuid, counter));
            counter++;
        }
    }

    /**
     * Store the TransactionData modifications in the TranscationStateMachine.
     *
     * For removed relationships/nodes, only the internal ID is provided. As we a UUID instead of the Neo4j id, we determine
     * deleted nodes by the removal of the UUID properties.
     *
     * @param transactionData
     */
    private void storeModifications(TransactionData transactionData) {

        TransactionStateMachine tsm = TmData.get();

        try (Transaction tx = db.beginTx()) {

            for (Node node : transactionData.createdNodes()) {
                String uuid = node.getProperty(Properties.UUID).toString();
                tsm.addCreatedNode(new ENode(uuid));
            }

            for (Node node : transactionData.deletedNodes()) {
                System.out.println("Deleted node: node_id= " + node.getId());
            }

            for (LabelEntry label : transactionData.removedLabels()) {
                System.out.println("Removed Label: " + label.node().getId() + label.label());
            }

            for (LabelEntry label : transactionData.assignedLabels()) {
                System.out.println("Assigned Label: " + label.node().getId() + label.label());
            }

            for (LabelEntry label : transactionData.removedLabels()) {
                System.out.println("Removed Label: " + label.node().getId() + label.label());
            }

            for (PropertyEntry<Node> property : transactionData.removedNodeProperties()) {
                System.out.println("Removed NodeProperty: " + property.entity() + " has value " + property.value());
            }

            for (PropertyEntry<Node> property : transactionData.assignedNodeProperties()) {
                System.out.println("Assigned NodeProperty: " + property.entity() + " has value " + property.value());
            }

            for (PropertyEntry<Node> property : transactionData.removedNodeProperties()) {
                System.out.println("Removed NodeProperty: " + property.entity() + " has value " + property.value());
            }

            for (PropertyEntry<Relationship> property : transactionData.assignedRelationshipProperties()) {
                System.out.println("Assigned RelationProperty: " + property.entity() + " has value " + property.value());
            }

            for (PropertyEntry<Relationship> property : transactionData.assignedRelationshipProperties()) {
                System.out.println("Removed RelationProperty: " + property.entity() + " has value " + property.value());
            }

            tx.success();
        }

        TmData.set(tsm);
    }

    /**
     * Check whether a property key was set or changed from in the transcation data.
     * Note that it doesn't check if the property was deleted.
     * @param transactionData   The transaction data with changes.
     * @param key               The key of the property
     * @return
     */
    private boolean hasPropertyChange(TransactionData transactionData, String key) {

        for (PropertyEntry<Relationship> property : transactionData.assignedRelationshipProperties()) {
            if (property.key() == key) {
                return true;
            }
        }

        for (PropertyEntry<Relationship> property : transactionData.removedRelationshipProperties()) {
            if (property.key() == key) {
                return true;
            }
        }

        for (PropertyEntry<Node> property : transactionData.assignedNodeProperties()) {
            if (property.key() == key) {
                return true;
            }
        }

        for (PropertyEntry<Node> property : transactionData.removedNodeProperties()) {
            if (property.key() == key) {
                return true;
            }
        }

        return false;
    }
}
