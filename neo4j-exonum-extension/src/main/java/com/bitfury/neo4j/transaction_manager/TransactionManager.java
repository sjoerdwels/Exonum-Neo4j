package com.bitfury.neo4j.transaction_manager;

import com.bitfury.neo4j.transaction_manager.exonum.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.LabelEntry;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.logging.Log;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class TransactionManager extends TransactionManagerGrpc.TransactionManagerImplBase {

    private static GraphDatabaseService db;
    private static LogService logService;
    private static Config config;

    private Log log;
    private Server gRPCServer;
    private TransactionManagerEventHandler eventHandler;

    ThreadLocal<TransactionStateMachine> TmData = new ThreadLocal<>();

    /**
     *
     *
     * @param db
     * @param config
     * @param logService
     */
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
     * @ref https://neo4j.com/docs/java-reference/current/transactions/ 7.6.1  - Neo4j transaction in a single thread.
     */
    private void handleTransaction(TransactionStateMachine.TransactionType type, TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {

        log.debug("method=handleTransaction type=" + type.name() + "totalQueries=" + request.getQueriesCount()
                + " threadID=" + Thread.currentThread().getId());

        try {

            TmData.set(new TransactionStateMachine(type, request.getUUIDPrefix()));

            if (request.getUUIDPrefix().isEmpty()) {
                TmData.get().failure();
                responseObserver.onNext(TmData.get().getTransactionErrorResponse(ErrorCode.EMPTY_UUID_PREFIX,"Transaction UUID is missing","",""));
                responseObserver.onCompleted();
                return;
            }

            if (request.getQueriesCount() == 0) {
                TmData.get().failure();
                responseObserver.onNext(TmData.get().getTransactionErrorResponse(ErrorCode.EMPTY_TRANSACTION,"Transaction has no insert queries","",""));
                responseObserver.onCompleted();
                return;
            }

            List<String> queries = request.getQueriesList();

            int queryCounter = 0;
            try (Transaction tx = db.beginTx()) {

                for (String query : queries) {
                    db.execute(query);
                    queryCounter++;
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
                System.out.println("Error  thrown " + ex.getMessage());
                TmData.get().failure();
                responseObserver.onNext(TmData.get().getTransactionErrorResponse(ErrorCode.FAILED_QUERIES,"Could not execute query",queries.get(queryCounter),ex.getMessage()));
                responseObserver.onCompleted();
                return;
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
     * Called by TransactionEventHandler before a Neo4j transaction is about to be committed.
     * Processes the transaction data whether needed.
     *
     * @param transactionData The changes that are about to be committed in this transaction.
     */
    public void beforeCommit(TransactionData transactionData) throws Exception {

        switch (TmData.get().getStatus()) {
            case INITIAL:

                if (hasPropertyChange(transactionData, Properties.UUID, false)) {
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
     * Store the TransactionData modifications in the TransactionStateMachine.
     * <p>
     * For removed relationships/nodes, the UUID is only available in the removedNodeProperties and
     * removedRelationshipProperties respectively. To retrieve the UUID based on
     * a node/relationship id, a lampda funciton is used that first  check if the UUID property was deleted.
     * Otherwise, the node/relationship still exists and it is retrieved from the graph database.
     *
     * @param transactionData
     */
    private void storeModifications(TransactionData transactionData) {

        TransactionStateMachine tsm = TmData.get();

        EUUID relationUUID = (txData, id) -> {

            Optional<PropertyEntry<Relationship>> entity = StreamSupport.stream(txData.removedRelationshipProperties().spliterator(), true)
                    .filter(p -> p.entity().getId() == id && p.key() == Properties.UUID)
                    .findFirst();

            if (entity.isPresent()) {
                return entity.get().previouslyCommitedValue().toString();
            }

            return db.getRelationshipById(id).getProperty(Properties.UUID).toString();
        };

        EUUID nodeUUID = (txData, id) -> {

            Optional<PropertyEntry<Node>> entity = StreamSupport.stream(txData.removedNodeProperties().spliterator(), true)
                    .filter(p -> p.entity().getId() == id && p.key() == Properties.UUID)
                    .findFirst();

            if (entity.isPresent()) {
                return entity.get().previouslyCommitedValue().toString();
            }

            return db.getNodeById(id).getProperty(Properties.UUID).toString();
        };


        try (Transaction tx = db.beginTx()) {

            for (Node node : transactionData.createdNodes()) {
                tsm.addCreatedNode(new ENode(nodeUUID.getUUID(transactionData, node.getId())));
            }

            for (Node node : transactionData.deletedNodes()) {
                tsm.addDeletedNode(new ENode(nodeUUID.getUUID(transactionData, node.getId())));
            }

            for (Relationship relationship : transactionData.createdRelationships()) {
                tsm.addCreatedRelationship(new ERelationship(
                        relationUUID.getUUID(transactionData, relationship.getId()),
                        relationship.getType().name(),
                        nodeUUID.getUUID(transactionData, relationship.getStartNodeId()),
                        nodeUUID.getUUID(transactionData, relationship.getEndNodeId())

                ));
            }

            for (Relationship relationship : transactionData.deletedRelationships()) {
                tsm.addDeletedRelationship(new ERelationship(
                        relationUUID.getUUID(transactionData, relationship.getId()),
                        relationship.getType().name(),
                        nodeUUID.getUUID(transactionData, relationship.getStartNodeId()),
                        nodeUUID.getUUID(transactionData, relationship.getEndNodeId())
                ));
            }

            for (LabelEntry label : transactionData.removedLabels()) {
                tsm.addRemovedLabel(new ELabel(
                        nodeUUID.getUUID(transactionData, label.node().getId()),
                        label.label().name())
                );
            }

            for (LabelEntry label : transactionData.assignedLabels()) {
                tsm.addAssignedLabel(new ELabel(
                        nodeUUID.getUUID(transactionData, label.node().getId()),
                        label.label().name())
                );
            }

            for (PropertyEntry<Node> property : transactionData.assignedNodeProperties()) {
                tsm.addAssignedNodeProperty(new EProperty(
                        nodeUUID.getUUID(transactionData, property.entity().getId()),
                        property.key(),
                        property.value().toString()
                ));
            }

            for (PropertyEntry<Node> property : transactionData.removedNodeProperties()) {
                tsm.addRemovedNodeProperty(new EProperty(
                        nodeUUID.getUUID(transactionData, property.entity().getId()),
                        property.key()
                ));
            }

            for (PropertyEntry<Relationship> property : transactionData.assignedRelationshipProperties()) {
                tsm.addAssignedRelationshipProperty(new EProperty(
                        relationUUID.getUUID(transactionData, property.entity().getId()),
                        property.key(),
                        property.value().toString()
                ));
            }

            for (PropertyEntry<Relationship> property : transactionData.removedRelationshipProperties()) {
                tsm.addRemovedRelationshipProperty(new EProperty(
                        relationUUID.getUUID(transactionData, property.entity().getId()),
                        property.key()
                ));
            }

            tx.failure();

        } catch (Exception ex) {
            System.out.println("Exception was thrown " + ex.getMessage());
        }

        TmData.set(tsm);

    }

    /**
     * Check whether a property key was set or changed from in the transaction data.
     * <p>
     * By default, if an entity is removed, all the assigned properties to that entity will be marked as removed.
     * If incEntityRemoved is false, it will not result in an property change if the entity was removed.
     *
     * @param txData           The transaction data with changes.
     * @param key              The key of the property
     * @param incEntityRemoved Boolean in case of the property entity is removed should result in a property change.
     * @return
     */
    private boolean hasPropertyChange(TransactionData txData, String key, boolean incEntityRemoved) {

        for (PropertyEntry<Relationship> property : txData.assignedRelationshipProperties()) {
            if (property.key() == key) {
                return true;
            }
        }

        for (PropertyEntry<Relationship> property : txData.removedRelationshipProperties()) {
            if (incEntityRemoved || (property.key() == key && !StreamSupport.stream(txData.deletedRelationships().spliterator(), true)
                    .anyMatch(n -> n.getId() == property.entity().getId()))) {
                return true;
            }
        }

        for (PropertyEntry<Node> property : txData.assignedNodeProperties()) {
            if (property.key() == key) {
                return true;
            }
        }

        for (PropertyEntry<Node> property : txData.removedNodeProperties()) {
            if (incEntityRemoved || (property.key() == key && !StreamSupport.stream(txData.deletedNodes().spliterator(), true)
                    .anyMatch(n -> n.getId() == property.entity().getId()))) {
                return true;
            }
        }

        return false;
    }
}
