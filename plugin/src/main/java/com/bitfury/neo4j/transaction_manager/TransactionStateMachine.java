package com.bitfury.neo4j.transaction_manager;

import com.bitfury.neo4j.transaction_manager.exonum.*;

import java.util.ArrayList;
import java.util.List;

public class TransactionStateMachine {

    public enum TransactionStatus {
        INITIAL,
        PENDING,
        READY_TO_COMMIT,
        COMMITTED,
        FAILED,
        FINISHED
    }

    private TransactionStatus status;
    private EError error;
    private String transactionID;

    // Database modifications
    private List<ENode> createdENodes = new ArrayList<>();
    private List<ENode> deletedENodes = new ArrayList<>();

    private List<ERelationship> createdERelationships = new ArrayList<>();
    private List<ERelationship> deletedERelationships = new ArrayList<>();

    private List<ELabel> assignedELabels = new ArrayList<>();
    private List<ELabel> removedELabels = new ArrayList<>();

    private List<EProperty> assignedNodeProperties = new ArrayList<>();
    private List<EProperty> removedNodeProperties = new ArrayList<>();

    private List<EProperty> assignedRelationshipProperties = new ArrayList<>();
    private List<EProperty> removedRelationshipProperties = new ArrayList<>();


    public TransactionStateMachine(String transactionID) {
        this.status = TransactionStatus.INITIAL;
        this.transactionID = transactionID;
        this.error = null;
    }

    public void pending() {
        this.status = TransactionStatus.PENDING;
    }

    public void readyToCommit() {
        this.status = TransactionStatus.READY_TO_COMMIT;
    }

    public void committed() {
        this.status = TransactionStatus.COMMITTED;
    }

    public void rolledBack() {
        this.status = TransactionStatus.FAILED;
    }

    public void failure(EError error) {
        this.status = TransactionStatus.FAILED;
        this.error = error;
    }

    public void finished() {
        this.status = TransactionStatus.FINISHED;
    }

    public TransactionStatus getStatus() {
        return this.status;
    }

    public String getTransactionID() {
        return this.transactionID;
    }

    public void addCreatedNode(ENode ENode) {
        this.createdENodes.add(ENode);
    }

    public void addDeletedNode(ENode ENode) {
        this.deletedENodes.add(ENode);
    }

    public void addCreatedRelationship(ERelationship ERelationship) {
        this.createdERelationships.add(ERelationship);
    }

    public void addDeletedRelationship(ERelationship ERelationship) {
        this.deletedERelationships.add(ERelationship);
    }

    public void addAssignedLabel(ELabel ELabel) {
        this.assignedELabels.add(ELabel);
    }

    public void addRemovedLabel(ELabel ELabel) {
        this.removedELabels.add(ELabel);
    }

    public void addAssignedNodeProperty(EProperty EProperty) {
        this.assignedNodeProperties.add(EProperty);
    }

    public void addRemovedNodeProperty(EProperty EProperty) {
        this.removedNodeProperties.add(EProperty);
    }

    public void addAssignedRelationshipProperty(EProperty EProperty) {
        this.assignedRelationshipProperties.add(EProperty);
    }

    public void addRemovedRelationshipProperty(EProperty EProperty) {
        this.removedRelationshipProperties.add(EProperty);
    }

    public boolean hasError() {
        return error != null;
    }

    public TransactionResponse getTransactionResponse() {

        TransactionResponse.Builder responseBuilder = TransactionResponse.newBuilder();
        responseBuilder.setTransactionId(this.transactionID);

        if (isSuccess()) {

            responseBuilder.setResult(Status.SUCCESS);

            DatabaseModifications.Builder modificationBuilder = DatabaseModifications.newBuilder();

            // Created nodes
            createdENodes.sort(null);
            for (ENode ENode : createdENodes) {
                modificationBuilder.addCreatedNodes(
                        DatabaseModifications.CreatedNode
                                .newBuilder()
                                .setNodeUUID(ENode.getUUID()).build()
                );
            }

            // Deleted nodes
            deletedENodes.sort(null);
            for (ENode ENode : deletedENodes) {
                modificationBuilder.addDeletedNodes(
                        DatabaseModifications.DeletedNode
                                .newBuilder()
                                .setNodeUUID(ENode.getUUID())
                );
            }

            // Created relationships
            createdERelationships.sort(null);
            for (ERelationship ERelationship : createdERelationships) {
                modificationBuilder.addCreatedRelationships(
                        DatabaseModifications.CreatedRelationShip
                                .newBuilder()
                                .setRelationshipUUID(ERelationship.getUUID())
                                .setRelationshipUUID(ERelationship.getUUID())
                                .setType(ERelationship.getType())
                                .setStartNodeUUID(ERelationship.getStartNodeUUID())
                                .setEndNodeUUID(ERelationship.getEndNodeUUID())
                );
            }

            // Deleted relationships
            deletedERelationships.sort(null);
            for (ERelationship ERelationship : deletedERelationships) {
                modificationBuilder.addDeletedRelationships(
                        DatabaseModifications.DeletedRelationship
                                .newBuilder()
                                .setRelationshipUUID(ERelationship.getUUID())
                );
            }

            // Assigned labels
            assignedELabels.sort(null);
            for (ELabel ELabel : assignedELabels) {
                modificationBuilder.addAssignedLabels(
                        DatabaseModifications.AssignedLabel
                                .newBuilder()
                                .setNodeUUID(ELabel.getNodeUUID())
                                .setName(ELabel.getName())
                );
            }

            // Removed labels
            removedELabels.sort(null);
            for (ELabel ELabel : removedELabels) {
                modificationBuilder.addRemovedLabels(
                        DatabaseModifications.RemovedLabel
                                .newBuilder()
                                .setNodeUUID(ELabel.getNodeUUID())
                                .setName(ELabel.getName())
                );
            }

            // Assigned node properties
            assignedNodeProperties.sort(null);
            for (EProperty EProperty : assignedNodeProperties) {

                DatabaseModifications.AssignedNodeProperty.Builder propertyBuilder =
                        DatabaseModifications.AssignedNodeProperty.newBuilder();

                propertyBuilder
                        .setNodeUUID(EProperty.getUUID())
                        .setKey(EProperty.getKey())
                        .setValue(EProperty.getValue());

                modificationBuilder.addAssignedNodeProperties(propertyBuilder);
            }

            // Removed node properties
            removedNodeProperties.sort(null);
            for (EProperty EProperty : removedNodeProperties) {

                modificationBuilder.addRemovedNodeProperties(
                        DatabaseModifications.RemovedNodeProperty.newBuilder()
                                .setNodeUUID(EProperty.getUUID())
                                .setKey(EProperty.getKey())

                );
            }


            // Assigned relationship properties
            assignedRelationshipProperties.sort(null);
            for (EProperty EProperty : assignedRelationshipProperties) {

                DatabaseModifications.AssignedRelationshipProperty.Builder propertyBuilder =
                        DatabaseModifications.AssignedRelationshipProperty.newBuilder();

                propertyBuilder
                        .setRelationshipUUID(EProperty.getUUID())
                        .setKey(EProperty.getKey())
                        .setValue(EProperty.getValue());

                modificationBuilder.addAssignedRelationshipProperties(propertyBuilder);
            }

            // Removed relationship properties
            removedRelationshipProperties.sort(null);
            for (EProperty EProperty : removedRelationshipProperties) {

                modificationBuilder.addRemovedRelationProperties(
                        DatabaseModifications.RemovedRelationshipProperty.newBuilder()
                                .setRelationshipUUID(EProperty.getUUID())
                                .setKey(EProperty.getKey())
                );
            }

            responseBuilder.setModifications(modificationBuilder);

        } else {

            responseBuilder.setResult(Status.FAILURE);

            if (hasError()) {
                Error.Builder errorBuilder = Error.newBuilder();

                switch (error.getType()) {
                    case FAILED_QUERY:
                        errorBuilder.setCode(ErrorCode.FAILED_QUERY);
                        break;
                    case MODIFIED_UUID:
                        errorBuilder.setCode(ErrorCode.MODIFIED_UUID);
                        break;
                    case EMPTY_TRANSACTION:
                        errorBuilder.setCode(ErrorCode.EMPTY_TRANSACTION);
                        break;
                    case EMPTY_UUID_PREFIX:
                        errorBuilder.setCode(ErrorCode.EMPTY_UUID_PREFIX);
                        break;
                    case RUNTIME_EXCEPTION:
                        errorBuilder.setCode(ErrorCode.RUNTIME_EXCEPTION);
                        break;
                    case TRANSACTION_ROLLBACK:
                        errorBuilder.setCode(ErrorCode.FAILED_QUERY);
                        break;
                    case CONSTRAINT_VIOLATION:
                        errorBuilder.setCode(ErrorCode.CONSTRAINT_VIOLATION);
                        break;
                }

                errorBuilder.setMessage(error.getMessage());

                if (error.hasFailedQuery()) {

                    EFailedQuery failedQuery = error.getFailedQuery();

                    FailedQuery.Builder failedQueryBuilder = FailedQuery.newBuilder();
                    failedQueryBuilder.setError(failedQuery.getError());
                    failedQueryBuilder.setQuery(failedQuery.getQuery());
                    errorBuilder.setFailedQuery(failedQueryBuilder);
                }

                responseBuilder.setError((errorBuilder));
            }
        }

        return responseBuilder.build();
    }

    private boolean isSuccess() {
        return this.status == TransactionStatus.FINISHED;
    }


}
