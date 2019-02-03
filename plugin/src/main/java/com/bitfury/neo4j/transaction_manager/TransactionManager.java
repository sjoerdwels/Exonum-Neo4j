package com.bitfury.neo4j.transaction_manager;

import com.bitfury.neo4j.transaction_manager.exonum.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.event.LabelEntry;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.logging.Log;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * @version 1.0
 * @authors Indrek Ermolaev, Silver Vapper, Sjoerd Wels
 */
public class TransactionManager extends TransactionManagerGrpc.TransactionManagerImplBase {

    private GraphDatabaseAPI db;

    private Log userLog;
    private Server gRPCServer;
    private Config config;

    ThreadLocal<TransactionStateMachine> TransactionData = new ThreadLocal<>();

    /**
     * @param db      The GraphDatabaseAPI
     * @param config  The Neo4j config object
     * @param userLog The log object for this file
     */
    TransactionManager(GraphDatabaseAPI db, Config config, Log userLog) {

        this.db = db;
        this.userLog = userLog;
        this.config = config;

        // Create folder to store block changes if not exists
        try {

            File f = getDatabaseChangesFolder();

            if (f.exists() && f.isDirectory()) {
                userLog.info("method=start message=block_changes folder already exists.");
            } else {
                if (f.mkdirs()) {
                    userLog.info("method=start message=block_changes folder created.");
                } else {
                    throw new Exception("method=start message=block_changes folder does not exists and could not be created");
                }
            }
        } catch (Exception e) {
            userLog.error("method=start error=Exception message=could not create block_changes folder result=shutdown database");
            db.shutdown();
        }

        int port = Properties.GRPC_DEFAULT_PORT;

        Optional<String> portConfig = config.getRaw(Properties.GRPC_KEY_PORT);
        if (portConfig.isPresent()) {
            try {
                port = Integer.parseInt(portConfig.get());
            } catch (NumberFormatException ex) {
                userLog.info("method=constructor error=NumberFormatException could not parse gRPC config port = " + portConfig.get());
            }
        }

        // Create gRPC server
        gRPCServer = ServerBuilder.forPort(port).addService(this).build();

        // Reset transaction state machine to initial state
        TransactionData.set(new TransactionStateMachine(null));

        userLog.info("method=constructor gRPCPort=" + port);

    }

    public void start() {

        // Start gRPC server
        try {
            userLog.info("method=start");
            gRPCServer.start();
        } catch (IOException e) {
            userLog.error("method=start error=IOException message=could not start gRPC server result=shutdown database");
            db.shutdown();
        }
    }

    public void shutdown() {
        userLog.info("method=shutdown");
        gRPCServer.shutdown();
    }

    /**
     * @param request
     * @param responseObserver
     */
    @Override
    public void executeBlock(BlockExecuteRequest request, StreamObserver<BlockExecuteResponse> responseObserver) {

        try {

            BlockChangesResponse.Builder blockChanges = BlockChangesResponse.newBuilder();
            blockChanges.setBlockId(request.getBlockId());

            if (request.getBlockId().isEmpty()) {
                userLog.error("method=executeBlock error=Missing block id");
                throw new Exception("Block ID missing");
            }

            List<TransactionRequest> transactions = request.getTransactionsList();

            // Execute each transaction
            for (TransactionRequest transaction : transactions) {
                TransactionResponse response = processTransaction(transaction);
                blockChanges.addTransactions(response);
            }

            // Store block changes in file
            try {

                File file = new File(getDatabaseChangesFolder(), request.getBlockId());
                FileOutputStream output = new FileOutputStream(file);
                blockChanges.build().writeTo(output);
                output.close();

            } catch (FileNotFoundException e) {
                userLog.error("method=BlockExecuteRequest error=FileNotFoundException message=could not create file for block_id=" + request.getBlockId());
                throw new Exception("Could not write changes to file");
            } catch (IOException e) {
                userLog.error("method=retrieveBlockChanges error=IOException block_id=" + request.getBlockId());
                throw new Exception("IOException in writing to file");
            }

            // Return result
            responseObserver.onNext(BlockExecuteResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(new StatusException(Status.INVALID_ARGUMENT.withDescription("Error = " + e.getMessage())));
        }
    }

    /**
     * @param request
     * @param responseObserver
     */
    @Override
    public void retrieveBlockChanges(BlockChangesRequest request, StreamObserver<BlockChangesResponse> responseObserver) {

        BlockChangesResponse.Builder builder = BlockChangesResponse.newBuilder();

        userLog.info("method=retrieveBlockChanges message=Retrieving block changes for block " + request.getBlockId());

        try {
            File file = new File(getDatabaseChangesFolder(), request.getBlockId());
            userLog.debug("method=retrieveBlockChanges filePath=" + file.getAbsolutePath());
            builder.mergeFrom(new FileInputStream(file));
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (FileNotFoundException e) {
            userLog.error("method=retrieveBlockChanges error=FileNotFoundException message=could not find block changes block_id=" + request.getBlockId());
            responseObserver.onError(new StatusException(io.grpc.Status.NOT_FOUND.withDescription("Could not find the changes for this block.")));
        } catch (IOException e) {
            userLog.error("method=retrieveBlockChanges error=IOException block_id=" + request.getBlockId());
            responseObserver.onError(new StatusException(io.grpc.Status.INTERNAL.withDescription("Internal IOException.")));
        }
    }

    /**
     * @param request
     * @param responseObserver
     */
    @Override
    public void deleteBlockChanges(DeleteBlockRequest request, StreamObserver<DeleteBlockResponse> responseObserver) {

        File file = new File(getDatabaseChangesFolder(), request.getBlockId());
        userLog.debug("method=deleteBlockChanges filePath=" + file.getAbsolutePath());

        if (file.exists() && file.isFile()) {

            if (file.delete()) {
                responseObserver.onNext(DeleteBlockResponse.newBuilder().setSuccess(true).build());
                userLog.info("method=deleteBlockRequest error=Deleted file for block_id=" + request.getBlockId());
            } else {
                responseObserver.onNext(DeleteBlockResponse.newBuilder().setSuccess(false).build());
                userLog.error("method=deleteBlockRequest error=File exists but could not delete file for block_id=" + request.getBlockId());
            }

        } else {
            responseObserver.onNext(DeleteBlockResponse.newBuilder().setSuccess(false).build());
            userLog.error("method=deleteBlockRequest message=File does not exists block_id=" + request.getBlockId());

        }

        responseObserver.onCompleted();
    }

    /**
     * Process a new transaction by executing the provided queries and assigning EUUID.
     * <p>
     * Creates a new thread local TmData object to store all related data to the transaction as the
     * TransactionEventHandler has no reference to the Transaction Manager instance. T
     *
     * @param request
     * @return
     * @throws Exception
     */
    private TransactionResponse processTransaction(TransactionRequest request) throws Exception {

        if (request.getTransactionId().isEmpty()) {
            userLog.error("method=processTransaction error=Missing transaction id");
            throw new Exception("Transaction ID missing");
        }

        userLog.info("method=processTransaction transactionID=" + request.getTransactionId() + " totalQueries=" + request.getQueriesCount());

        TransactionData.set(new TransactionStateMachine(request.getTransactionId()));

        TransactionData.get().pending();

        List<String> queries = request.getQueriesList();

        try (Transaction tx = db.beginTx()) {

            for (String query : queries) {
                try {
                    db.execute(query);
                } catch (QueryExecutionException ex) {

                    TransactionData.get().failure(
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

            if (!TransactionData.get().hasError()) {
                tx.success();
            }

        } catch (TransactionFailureException ex) {
            /* If the transaction was rolled back in this extension, the error is already provided.
                Otherwise, an external extension prevented the transaction to be committed.
             */

            if (!TransactionData.get().hasError()) {
                TransactionData.get().failure(
                        new EError(EError.ErrorType.TRANSACTION_ROLLBACK, "Transaction was ready to be committed but rolled back.")
                );
            }
        } catch (ConstraintViolationException ex) {

            TransactionData.get().failure(
                    new EError(EError.ErrorType.CONSTRAINT_VIOLATION, ex.getMessage())
            );

        } catch (Exception ex) {

           TransactionData.get().failure(
                    new EError(EError.ErrorType.RUNTIME_EXCEPTION, "Runtime exception:  " + ex.getMessage())
            );
        }

        return TransactionData.get().getTransactionResponse();
    }

    /**
     * Called by TransactionEventHandler before a Neo4j transaction is about to be committed.
     * Processes the transaction data whether needed.
     *
     * @param transactionData The changes that are about to be committed in this transaction.
     */
    public void beforeCommit(TransactionData transactionData) throws Exception {

        switch (TransactionData.get().getStatus()) {
            case INITIAL:
                userLog.debug("method=beforeCommit, TmData.status=" + TransactionStateMachine.TransactionStatus.INITIAL + ", no gRPC transaction.");
                throw new Exception("No gRPC transaction started, external transactions not allowed. ");
            case PENDING:

                if (hasPropertyChanged(transactionData, Properties.UUID, false)) {

                    TransactionData.get().failure(
                            new EError(EError.ErrorType.MODIFIED_UUID, "Transaction tried to modify UUID properties.")
                    );
                    throw new Exception("A query tried to modify a UUID, which is not allowed.");
                }

                assignUUIDS(transactionData);

                TransactionData.get().readyToCommit();

                break;
            default:
                userLog.debug("method=afterCommit, TmData.status=" + TransactionData.get().getStatus() + ", transaction data not  processed");
        }
    }

    /**
     * Called by TransactionEventHandler after a Neo4j transaction has successfully been committed.
     * Processes the transaction data whether needed.
     *
     * @param transactionData The changes that were committed in this transaction.
     */
    public void afterCommit(TransactionData transactionData) {

        switch (TransactionData.get().getStatus()) {
            case INITIAL:

                userLog.error("method=afterCommit, TmData.status=" + TransactionData.get().getStatus() + ", external transactions should not be allowed.");
                break;

            case READY_TO_COMMIT:

                TransactionData.get().committed();

                storeTransactionModifications(transactionData);

                TransactionData.get().finished();
                break;
            default:
                userLog.debug("method=afterCommit, TmData.status=" + TransactionData.get().getStatus() + ", transaction data not  processed");
        }
    }

    /**
     * Called after transaction has been rolled back because committing the transaction failed.
     * Could be caused by external Transaction Event Handlers
     *
     * @param transactionData The changes that were attempted to be committed in this transaction.
     */
    @SuppressWarnings("unused")
    public void afterRollback(TransactionData transactionData) {
        TransactionData.get().rolledBack();
    }

    /**
     * @param transactionData The transaction data including all transaction changes
     */
    private void assignUUIDS(TransactionData transactionData) {

        String transaction_id = TransactionData.get().getTransactionID();

        AtomicInteger atomicCounter = new AtomicInteger(0);

        StreamSupport.stream(transactionData.createdNodes().spliterator(), false).sorted(new NodeComparator()).forEach(node -> {
            node.setProperty(Properties.UUID, Properties.concatUUID(transaction_id, atomicCounter.getAndIncrement()));
        });

        StreamSupport.stream(transactionData.createdRelationships().spliterator(), false).sorted(new RelationshipComparator()).forEach(relationship -> {
            relationship.setProperty(Properties.UUID, Properties.concatUUID(transaction_id, atomicCounter.getAndIncrement()));
        });
    }

    /**
     * Store the TransactionData modifications in the TransactionStateMachine.
     * <p>
     * For removed relationships/nodes, the UUID is only available in the removedNodeProperties and
     * removedRelationshipProperties respectively. To retrieve the UUID based on
     * a node/relationship id, a lambda function is used that first  check if the UUID property was deleted.
     * Otherwise, the node/relationship still exists and it is retrieved from the graph database.
     *
     * @param transactionData       The TransactionData that needs to be stored in the state  machine
     */
    private void storeTransactionModifications(TransactionData transactionData) {

        TransactionStateMachine tsm = TransactionData.get();

        EUUID relationUUID = (txData, id) -> {

            Optional<PropertyEntry<Relationship>> entity = StreamSupport.stream(txData.removedRelationshipProperties().spliterator(), true)
                    .filter(p -> p.entity().getId() == id && Objects.equals(p.key(), Properties.UUID))
                    .findFirst();

            if (entity.isPresent()) {
                return entity.get().previouslyCommitedValue().toString();
            }

            return db.getRelationshipById(id).getProperty(Properties.UUID).toString();
        };

        EUUID nodeUUID = (txData, id) -> {

            Optional<PropertyEntry<Node>> entity = StreamSupport.stream(txData.removedNodeProperties().spliterator(), true)
                    .filter(p -> p.entity().getId() == id && Objects.equals(p.key(), Properties.UUID))
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

                if (!property.key().equals(Properties.UUID)) {
                    tsm.addRemovedNodeProperty(new EProperty(
                            nodeUUID.getUUID(transactionData, property.entity().getId()),
                            property.key()
                    ));
                }
            }

            for (PropertyEntry<Relationship> property : transactionData.assignedRelationshipProperties()) {

                tsm.addAssignedRelationshipProperty(new EProperty(
                        relationUUID.getUUID(transactionData, property.entity().getId()),
                        property.key(),
                        property.value().toString()
                ));

            }

            for (PropertyEntry<Relationship> property : transactionData.removedRelationshipProperties()) {
                if (!property.key().equals(Properties.UUID)) {
                    tsm.addRemovedRelationshipProperty(new EProperty(
                            relationUUID.getUUID(transactionData, property.entity().getId()),
                            property.key()
                    ));
                }
            }

            tx.success();
        }

        TransactionData.set(tsm);
    }

    /**
     * Check whether a property key was set or changed based on the transaction data.
     * <p>
     * By default, if an entity is removed, all the assigned properties to that entity will be marked as removed.
     * If incEntityRemoved is false, it will not result in an property change if the entity of the property was removed
     * as well.
     *
     * @param txData           The transaction data with changes.
     * @param key              The key of the property
     * @param incEntityRemoved Boolean in case of the property entity is removed should result in a property change.
     * @return Whether a property key was set or changed
     */
    private boolean hasPropertyChanged(TransactionData txData, String key, boolean incEntityRemoved) {

        for (PropertyEntry<Relationship> property : txData.assignedRelationshipProperties()) {
            if (Objects.equals(property.key(), key)) {
                return true;
            }
        }

        for (PropertyEntry<Relationship> property : txData.removedRelationshipProperties()) {
            if (incEntityRemoved || (Objects.equals(property.key(), key) && !StreamSupport.stream(txData.deletedRelationships().spliterator(), true)
                    .anyMatch(n -> n.getId() == property.entity().getId()))) {
                return true;
            }
        }

        for (PropertyEntry<Node> property : txData.assignedNodeProperties()) {
            if (Objects.equals(property.key(), key)) {
                return true;
            }
        }

        for (PropertyEntry<Node> property : txData.removedNodeProperties()) {
            if (incEntityRemoved || (Objects.equals(property.key(), key) && !StreamSupport.stream(txData.deletedNodes().spliterator(), true)
                    .anyMatch(n -> n.getId() == property.entity().getId()))) {
                return true;
            }
        }

        return false;
    }


    private File getDatabaseChangesFolder() {

        Optional<Object> value = config.getValue("dbms.directories.data");

        if (!value.isPresent()) {
            userLog.error("method=start error=Exception message=could not determine Neo4j data folder");
            db.shutdown();
        }

        return new File(value.get().toString(), Properties.DATABASE_CHANGES_FOLDER);

    }
}
