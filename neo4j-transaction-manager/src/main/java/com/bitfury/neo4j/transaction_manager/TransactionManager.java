package com.bitfury.neo4j.transaction_manager;

import com.bitfury.neo4j.transaction_manager.exonum.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.event.LabelEntry;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.logging.Log;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class TransactionManager extends TransactionManagerGrpc.TransactionManagerImplBase {

    private GraphDatabaseAPI db;

    private Log userLog;
    private Server gRPCServer;

    ThreadLocal<RequestStateMachine> TmData = new ThreadLocal<>();

    /**
     * @param db
     * @param config
     * @param userLog
     */
    TransactionManager(GraphDatabaseAPI db, Config config, Log userLog) {

        this.db = db;
        this.userLog = userLog;

        int port = Properties.GRPC_DEFAULT_PORT;

        Optional<String> portConfig =  config.getRaw(Properties.GRPC_KEY_PORT);
        if(portConfig.isPresent()) {
            try {
                port = Integer.parseInt(portConfig.get());
            }catch (NumberFormatException ex){
                userLog.info("method=constructor error=NumberFormatException could not parse gRPC config port = " + portConfig.get());
            }
        }

        // Create gRPC server
        gRPCServer = ServerBuilder.forPort(port).addService(this).build();

        userLog.info("method=constructor gRPCPort=" + port);
    }

    public void start() {

        try {
            userLog.info("method=start");
            gRPCServer.start();
        } catch (IOException e) {
            userLog.error("method=start error=IOException shutdown database");
            db.shutdown();
        }
    }

    public void shutdown() {
        userLog.info("method=shutdown");
        gRPCServer.shutdown();
    }

    @Override
    public void verify(TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        userLog.info("method=verifyTransaction request=" + request.toString());
        processGRPCRequest(RequestStateMachine.TransactionType.VERIFY, request, responseObserver);
    }

    @Override
    public void execute(TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {
        userLog.info("method=executeTransaction request=" + request.toString());
        processGRPCRequest(RequestStateMachine.TransactionType.EXECUTE, request, responseObserver);
    }

    /**
     * Process a new gRPC request.
     * <p>
     * Creates a new thread local TmData object to store all related data to the gRPC request as the
     * TransactionEventHandler has no reference to the gRPC executor instance. Then processes the
     * request message and returns the result to the observer.
     *
     * @param type             The gRPC method type
     * @param request          The request message
     * @param responseObserver The responseObserver
     */
    private void processGRPCRequest(RequestStateMachine.TransactionType type, TransactionRequest request, StreamObserver<TransactionResponse> responseObserver) {

        TmData.set(new RequestStateMachine(type, request.getUUIDPrefix()));

        processRequestMessage(request);

        responseObserver.onNext(TmData.get().getTransactionResponse());
        responseObserver.onCompleted();
    }

    /**
     * Process a new transaction request by executing the provided queries.
     *
     * @param request The request message
     */
    private void processRequestMessage(TransactionRequest request) {

        userLog.info("method=executeRequest type=" + TmData.get().getTransactionType() + "totalQueries=" + request.getQueriesCount()
                + " threadID=" + Thread.currentThread().getId());

        if (request.getUUIDPrefix().isEmpty() && TmData.get().getTransactionType()==RequestStateMachine.TransactionType.EXECUTE) {
            TmData.get().failure(
                    new EError(EError.ErrorType.EMPTY_UUID_PREFIX, "Transaction is missing UUID prefix.")
            );
            return;
        }

        if (request.getQueriesCount() == 0) {
            TmData.get().failure(
                    new EError(EError.ErrorType.EMPTY_TRANSACTION, "No queries provided.")
            );
            return;
        }

        List<String> queries = request.getQueriesList();

        try (Transaction tx = db.beginTx()) {

            for (String query : queries) {
                try {
                    db.execute(query);
                } catch (QueryExecutionException ex) {

                    TmData.get().failure(
                            new EError(
                                    new EFailedQuery(
                                            query,
                                            ex.getMessage(),
                                            ex.getStatusCode()
                                    )
                            )
                    );
                    break;
                }
            }

            if (!TmData.get().hasError() && TmData.get().getTransactionType() == RequestStateMachine.TransactionType.EXECUTE) {
                tx.success();
            }

        } catch (TransactionFailureException ex) {
            /* If the transaction was rolled back in this extension, the error is already provided.
                Otherwise, an external extension prevented the transaction to be committed.
             */
            if (!TmData.get().hasError()) {
                TmData.get().failure(
                        new EError(EError.ErrorType.TRANSACTION_ROLLBACK, "Transaction was ready to be committed but rolled back.")
                );
            }

        } catch (Exception ex) {
            TmData.get().failure(
                    new EError(EError.ErrorType.RUNTIME_EXCEPTION, "Runtime exception:  " + ex.getMessage())
            );
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
// todo d
                if (hasPropertyChanged(transactionData, Properties.UUID, false)) {

                    TmData.get().failure(
                            new EError(EError.ErrorType.MODIFIED_UUID, "Transaction tried to modify UUID properties.")
                    );
                    throw new Exception("A query tried to modify a UUID, which is not allowed.");
                }

                assignUUIDS(transactionData);

                TmData.get().readyToCommit();

                break;
            default:
                userLog.debug("method=afterCommit, TmData.status=" + TmData.get().getStatus() + ", transaction data not  processed");
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
                userLog.debug("method=afterCommit, TmData.status=" + TmData.get().getStatus() + ", transaction data not  processed");
        }
    }

    /**
     * Called after transaction has been rolled back because committing the transaction failed.
     * Could be caused by external Transaction Event Handlers
     *
     * @param transactionData The changes that were attempted to be committed in this transaction.
     */
    public void afterRollback(TransactionData transactionData) {
        TmData.get().rolledBack();
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
     * Store the TransactionData modifications in the RequestStateMachine.
     * <p>
     * For removed relationships/nodes, the UUID is only available in the removedNodeProperties and
     * removedRelationshipProperties respectively. To retrieve the UUID based on
     * a node/relationship id, a lambda function is used that first  check if the UUID property was deleted.
     * Otherwise, the node/relationship still exists and it is retrieved from the graph database.
     *
     * @param transactionData
     */
    // todo
    private void storeModifications(TransactionData transactionData) {

        RequestStateMachine tsm = TmData.get();

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

            tx.success();

        } catch (Exception ex) {
            System.out.println("Exception was thrown " + ex.getMessage());
        }

        TmData.set(tsm);

    }

    /**
     * Check whether a property key was set or changed based on the transaction data.
     * <p>
     * By default, if an entity is removed, all the assigned properties to that entity will be marked as removed.
     * If incEntityRemoved is false, it will not result in an property change if the entity of the property was removed
     *  as well.
     *
     * @param txData           The transaction data with changes.
     * @param key              The key of the property
     * @param incEntityRemoved Boolean in case of the property entity is removed should result in a property change.
     * @return
     */
    private boolean hasPropertyChanged(TransactionData txData, String key, boolean incEntityRemoved) {

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
