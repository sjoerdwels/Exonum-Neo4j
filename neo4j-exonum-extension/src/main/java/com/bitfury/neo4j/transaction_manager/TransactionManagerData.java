package com.bitfury.neo4j.transaction_manager;

import java.util.ArrayList;
import java.util.List;

import com.bitfury.neo4j.transaction_manager.graphdb.*;

import javax.xml.crypto.Data;

public class TransactionManagerData {

    public enum TransactionType {
        VERIFY,
        EXECUTE
    }

    private TransactionType transactionType;
    private boolean isCommitted = false;
    private boolean isRolledback = false;
    private boolean failure = false;

    // Database modifications
    private List<Node> createdNodes = new ArrayList<>();
    private List<Node> deletedNodes = new ArrayList<>();

    private List<Relationship> createdRelationships = new ArrayList<>();
    private List<Relationship> deletedRelationships = new ArrayList<>();

    private List<Label> assignedLabels = new ArrayList<>();
    private List<Label> removedLabels = new ArrayList<>();

    private List<Property> assignedNodeProperties = new ArrayList<>();
    private List<Property> removedNodeProperties = new ArrayList<>();

    private List<Property> assignedRelationshipProperties = new ArrayList<>();
    private List<Property> removedRelationshipProperties = new ArrayList<>();


    public TransactionManagerData(TransactionType type) {
        transactionType = type;
    }

    public void isCommitted() {
        isCommitted = true;
    }

    public void isRolledback() {
        isRolledback = true;
    }

    public void failure() {
        failure = true;
    }

    public TransactionResponse getTransactionResponse() {

        TransactionResponse.Builder responseBuilder = TransactionResponse.newBuilder().setResult(getStatus());

        if (isCommitted) {

            DatabaseModifications.Builder modificationBuilder = DatabaseModifications.newBuilder();

            // Created nodes
            for (Node node : createdNodes) {
                modificationBuilder.addCreatedNodes(
                        DatabaseModifications.CreatedNode
                                .newBuilder()
                                .setNodeUUID(node.getUUID()).build()
                );
            }

            // Deleted nodes
            for (Node node : deletedNodes) {
                modificationBuilder.addDeletedNodes(
                        DatabaseModifications.DeletedNode
                                .newBuilder()
                                .setNodeUUID(node.getUUID())
                );
            }

            // Created relationships
            for (Relationship relationship : createdRelationships) {
                modificationBuilder.addCreatedRelationships(
                        DatabaseModifications.CreatedRelationShip
                                .newBuilder()
                                .setRelationshipUUID(relationship.getUUID())
                                .setRelationshipUUID(relationship.getUUID())
                                .setType(relationship.getType())
                                .setStartNodeUUID(relationship.getStartNodeUUID())
                                .setEndNodeUUID(relationship.getEndNodeUUID())
                );
            }

            // Deleted relationships
            for (Relationship relationship : deletedRelationships) {
                modificationBuilder.addDeletedRelationships(
                        DatabaseModifications.DeletedRelationship
                                .newBuilder()
                               .setRelationshipUUID(relationship.getUUID())
                );
            }

            // Assigned labels
            for (Label label : assignedLabels) {
                modificationBuilder.addAssignedLabels(
                        DatabaseModifications.AssignedLabel
                                .newBuilder()
                                .setNodeUUID(label.getNodeUUID())
                                .setName(label.getName())
                );
            }

            // Removed labels
            for (Label label : removedLabels) {
                modificationBuilder.addRemovedLabels(
                        DatabaseModifications.RemovedLabel
                                .newBuilder()
                                .setNodeUUID(label.getNodeUUID())
                                .setName(label.getName())
                );
            }

            // Assigned node properties
            for (Property property : assignedNodeProperties) {

                DatabaseModifications.AssignedNodeProperty.Builder propertyBuilder =
                        DatabaseModifications.AssignedNodeProperty.newBuilder();

                propertyBuilder
                        .setNodeUUID(property.getUUID())
                        .setKey(property.getKey())
                        .setValue(property.getValue());

                if( property.getPreviousValue() != null) {
                    propertyBuilder.setPreviousValue(property.getPreviousValue());
                }

                modificationBuilder.addAssignedNodeProperties(propertyBuilder);
            }

            // Removed node properties
            for (Property property : removedNodeProperties) {

                modificationBuilder.addRemovedNodeProperties(
                        DatabaseModifications.RemovedNodeProperty.newBuilder()
                                .setNodeUUID(property.getUUID())
                                .setKey(property.getKey())

                );
            }


            // Assigned relationship properties
            for (Property property : assignedRelationshipProperties) {

                DatabaseModifications.AssignedRelationshipProperty.Builder propertyBuilder =
                        DatabaseModifications.AssignedRelationshipProperty.newBuilder();

                propertyBuilder
                        .setRelationshipUUID(property.getUUID())
                        .setKey(property.getKey())
                        .setValue(property.getValue());

                if( property.getPreviousValue() != null) {
                    propertyBuilder.setPreviousValue(property.getPreviousValue());
                }

                modificationBuilder.addAssignedRelationshipProperties(propertyBuilder);
            }

            // Removed relationship properties
            for (Property property : removedRelationshipProperties) {

                modificationBuilder.addRemovedRelationProperties(
                        DatabaseModifications.RemovedRelationshipProperty.newBuilder()
                                .setRelationshipUUID(property.getUUID())

                );
            }

            responseBuilder.setModifications(modificationBuilder);
        }

        return responseBuilder.build();
    }

    private Status getStatus() {

        boolean success = false;

        switch (this.transactionType) {
            case VERIFY:
                success = !failure;
                break;
            case EXECUTE:
                success = isCommitted && !isRolledback;
                break;
        }

        return success ? Status.SUCCESS : Status.FAILURE;
    }

}
