package com.bitfury.neo4j.transaction_manager;

import com.bitfury.neo4j.transaction_manager.exonum.ELabel;
import com.bitfury.neo4j.transaction_manager.exonum.ENode;
import com.bitfury.neo4j.transaction_manager.exonum.EProperty;
import com.bitfury.neo4j.transaction_manager.exonum.ERelationship;

import java.util.ArrayList;
import java.util.List;

import com.bitfury.neo4j.transaction_manager.exonum.*;

public class TransactionStateMachine {

    public enum TransactionType {
        VERIFY,
        EXECUTE
    }

    public enum TransactionStatus {
        INITIAL,
        READY_TO_COMMIT,
        COMMITTED,
        FAILED,
        ASSIGNED_UUIDS,
        STORED_MODIFICATIONS,
        FINISHED
    }

    private TransactionStatus status;
    private TransactionType transactionType;
    private String uuidPrefix;

    private boolean isCommitted = false;

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


    public TransactionStateMachine(TransactionType transactionType, String uuid_prefix) {
        this.status = TransactionStatus.INITIAL;
        this.transactionType = transactionType;
        this.uuidPrefix = uuid_prefix;
    }


    public void readyToCommit() { this.status = TransactionStatus.READY_TO_COMMIT; }

    public void committed() { this.status = TransactionStatus.COMMITTED; this.isCommitted = true;}

    public void rolledback() { this.status = TransactionStatus.FAILED; }

    public void failure() { this.status = TransactionStatus.FAILED; }

    public void assignedUUIDs() { this.status = TransactionStatus.ASSIGNED_UUIDS; }

    public void storedModifications() { this.status = TransactionStatus.STORED_MODIFICATIONS; }

    public void finished() { this.status = TransactionStatus.FINISHED; }

    public TransactionStatus getStatus() { return this.status; }

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

    public void addAsignedLabel(ELabel ELabel) {
        this.assignedELabels.add(ELabel);
    }

    public void addRemovedLabel(ELabel ELabel) {
        this.removedELabels.add(ELabel);
    }

    public void addaAsignedNodeProperty(EProperty EProperty) {
        this.assignedNodeProperties.add(EProperty);
    }

    public void addRemovedNodeProperty(EProperty EProperty) {
        this.removedNodeProperties.add(EProperty);
    }

    public void addaAssignedRelationshipProperty(EProperty EProperty) {
        this.assignedRelationshipProperties.add(EProperty);
    }

    public void addRemovedRelationshipProperty(EProperty EProperty) {
        this.removedRelationshipProperties.add(EProperty);
    }

    public String getUuidPrefix() {
        return uuidPrefix;
    }

    public TransactionResponse getTransactionResponse() {

        TransactionResponse.Builder responseBuilder = TransactionResponse.newBuilder().setResult(getTranscationResponseStatus());

        if (isCommitted) {

            DatabaseModifications.Builder modificationBuilder = DatabaseModifications.newBuilder();

            // Created nodes
            for (ENode ENode : createdENodes) {
                modificationBuilder.addCreatedNodes(
                        DatabaseModifications.CreatedNode
                                .newBuilder()
                                .setNodeUUID(ENode.getUUID()).build()
                );
            }

            // Deleted nodes
            for (ENode ENode : deletedENodes) {
                modificationBuilder.addDeletedNodes(
                        DatabaseModifications.DeletedNode
                                .newBuilder()
                                .setNodeUUID(ENode.getUUID())
                );
            }

            // Created relationships
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
            for (ERelationship ERelationship : deletedERelationships) {
                modificationBuilder.addDeletedRelationships(
                        DatabaseModifications.DeletedRelationship
                                .newBuilder()
                                .setRelationshipUUID(ERelationship.getUUID())
                );
            }

            // Assigned labels
            for (ELabel ELabel : assignedELabels) {
                modificationBuilder.addAssignedLabels(
                        DatabaseModifications.AssignedLabel
                                .newBuilder()
                                .setNodeUUID(ELabel.getNodeUUID())
                                .setName(ELabel.getName())
                );
            }

            // Removed labels
            for (ELabel ELabel : removedELabels) {
                modificationBuilder.addRemovedLabels(
                        DatabaseModifications.RemovedLabel
                                .newBuilder()
                                .setNodeUUID(ELabel.getNodeUUID())
                                .setName(ELabel.getName())
                );
            }

            // Assigned node properties
            for (EProperty EProperty : assignedNodeProperties) {

                DatabaseModifications.AssignedNodeProperty.Builder propertyBuilder =
                        DatabaseModifications.AssignedNodeProperty.newBuilder();

                propertyBuilder
                        .setNodeUUID(EProperty.getUUID())
                        .setKey(EProperty.getKey())
                        .setValue(EProperty.getValue());

                if (EProperty.getPreviousValue() != null) {
                    propertyBuilder.setPreviousValue(EProperty.getPreviousValue());
                }

                modificationBuilder.addAssignedNodeProperties(propertyBuilder);
            }

            // Removed node properties
            for (EProperty EProperty : removedNodeProperties) {

                modificationBuilder.addRemovedNodeProperties(
                        DatabaseModifications.RemovedNodeProperty.newBuilder()
                                .setNodeUUID(EProperty.getUUID())
                                .setKey(EProperty.getKey())

                );
            }


            // Assigned relationship properties
            for (EProperty EProperty : assignedRelationshipProperties) {

                DatabaseModifications.AssignedRelationshipProperty.Builder propertyBuilder =
                        DatabaseModifications.AssignedRelationshipProperty.newBuilder();

                propertyBuilder
                        .setRelationshipUUID(EProperty.getUUID())
                        .setKey(EProperty.getKey())
                        .setValue(EProperty.getValue());

                if (EProperty.getPreviousValue() != null) {
                    propertyBuilder.setPreviousValue(EProperty.getPreviousValue());
                }

                modificationBuilder.addAssignedRelationshipProperties(propertyBuilder);
            }

            // Removed relationship properties
            for (EProperty EProperty : removedRelationshipProperties) {

                modificationBuilder.addRemovedRelationProperties(
                        DatabaseModifications.RemovedRelationshipProperty.newBuilder()
                                .setRelationshipUUID(EProperty.getUUID())
                );
            }

            responseBuilder.setModifications(modificationBuilder);
        }

        return responseBuilder.build();
    }

    private Status getTranscationResponseStatus() {

        boolean success = false;

        switch (this.transactionType) {
            case VERIFY:
                success = this.status == TransactionStatus.READY_TO_COMMIT;
                break;
            case EXECUTE:
                success = this.status == TransactionStatus.FINISHED;
                break;
        }

        return success ? Status.SUCCESS : Status.FAILURE;
    }

}
