package com.bitfury.neo4j.transaction_manager;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.HashMap;

import static org.junit.Assert.*;

@Ignore
public class ModificationsTest {

    private GraphDatabaseService db;
    private static int block_id = 0;
    private static int tx_id = 0;

    static final int TEST_GRPC_PORT = Properties.GRPC_DEFAULT_PORT;

    private ManagedChannel channel;
    private static TransactionManagerGrpc.TransactionManagerBlockingStub blockingStub;


    // Test data
    HashMap<Integer, String> testUUIDs = new HashMap<>();
    public static final int PERSON_WITH_RELATION = 0;
    public static final int PERSON_NO_RELATION = 1;
    public static final int ADDRESS = 2;
    public static final int LIVES_IN = 3;
    public static final int NODE_NO_LABEL = 4;

    @Before
    @SuppressWarnings( "deprecation" ) // The settings API will be completely rewritten in 4.0
    public void setUp() {
        db = new TestGraphDatabaseFactory()
                .newImpermanentDatabaseBuilder()
                .setConfig(Properties.GRPC_KEY_PORT, String.valueOf(TEST_GRPC_PORT))
                .newGraphDatabase();

        channel = ManagedChannelBuilder.forAddress("localhost", TEST_GRPC_PORT)
                .usePlaintext()
                .build();

        blockingStub = TransactionManagerGrpc.newBlockingStub(channel);

        block_id++;

        insertTestData();
    }

    @After
    public void tearDown() {
        channel.shutdown();
        db.shutdown();
    }


    @Test
    public void assignNodeUUID() {

        TransactionResponse result = sendQuery("CREATE (n:Person { " + Properties.UUID + ": 'customUUID'})");

        assertEquals("Query should fail", Status.FAILURE, result.getResult());
        assertEquals("Query should have an error", true, result.hasError());
        assertEquals("Incorrect error", ErrorCode.MODIFIED_UUID, result.getError().getCode());
        assertEquals("Query should not result in changes", false, result.hasModifications());
    }

    @Test
    public void modifyNodeUUID() {

        TransactionResponse result = sendQuery("MATCH (n:Person) SET n." + Properties.UUID + " =  'modifiedUUID'");

        assertEquals("Query should fail", Status.FAILURE, result.getResult());
        assertEquals("Incorrect error", ErrorCode.MODIFIED_UUID, result.getError().getCode());
        assertEquals("Query should not result in changes", false, result.hasModifications());

    }

    @Test
    public void testCreateNode() {

        TransactionResponse result = sendQuery("CREATE (n)");

        assertEquals("Query should succeed", Status.SUCCESS, result.getResult());
        assertEquals("Query should not have errors", false, result.hasError());
        assertEquals("Query should have changes", true, result.hasModifications());

        assertEquals("Incorrect number of nodes created.", 1, result.getModifications().getCreatedNodesCount());
        assertEquals("Incorrect number of nodes deleted.", 0, result.getModifications().getDeletedNodesCount());
        assertEquals("Incorrect number of relationships created.", 0, result.getModifications().getCreatedRelationshipsCount());
        assertEquals("Incorrect number of relationships deleted.", 0, result.getModifications().getDeletedRelationshipsCount());
        assertEquals("Incorrect number of assigned relationship properties.", 0, result.getModifications().getAssignedRelationshipPropertiesCount());
        assertEquals("Incorrect number of removed relationship properties.", 0, result.getModifications().getRemovedRelationPropertiesCount());
        assertEquals("Incorrect number of assigned node properties.", 0, result.getModifications().getAssignedNodePropertiesCount());
        assertEquals("Incorrect number of removed node properties.", 0, result.getModifications().getRemovedNodePropertiesCount());
        assertEquals("Incorrect number of assigned labels.", 0, result.getModifications().getAssignedLabelsCount());
        assertEquals("Incorrect number of removed labels.", 0, result.getModifications().getRemovedLabelsCount());

        DatabaseModifications.CreatedNode createdNode = result.getModifications().getCreatedNodes(0);

        assertEquals("Empty node UUID", false, createdNode.getNodeUUID().isEmpty());

    }

    @Test
    public void testCreateNodeWithLabel() {

        TransactionResponse result = sendQuery("CREATE (n:Label)");

        assertEquals("Query should succeed", Status.SUCCESS, result.getResult());
        assertEquals("Query should not have errors", false, result.hasError());
        assertEquals("Query should have changes", true, result.hasModifications());

        assertEquals("Incorrect number of nodes created.", 1, result.getModifications().getCreatedNodesCount());
        assertEquals("Incorrect number of nodes deleted.", 0, result.getModifications().getDeletedNodesCount());
        assertEquals("Incorrect number of relationships created.", 0, result.getModifications().getCreatedRelationshipsCount());
        assertEquals("Incorrect number of relationships deleted.", 0, result.getModifications().getDeletedRelationshipsCount());
        assertEquals("Incorrect number of assigned relationship properties.", 0, result.getModifications().getAssignedRelationshipPropertiesCount());
        assertEquals("Incorrect number of removed relationship properties.", 0, result.getModifications().getRemovedRelationPropertiesCount());
        assertEquals("Incorrect number of assigned node properties.", 0, result.getModifications().getAssignedNodePropertiesCount());
        assertEquals("Incorrect number of removed node properties.", 0, result.getModifications().getRemovedNodePropertiesCount());
        assertEquals("Incorrect number of assigned labels.", 1, result.getModifications().getAssignedLabelsCount());
        assertEquals("Incorrect number of removed labels.", 0, result.getModifications().getRemovedLabelsCount());

        DatabaseModifications.CreatedNode createdNode = result.getModifications().getCreatedNodes(0);
        DatabaseModifications.AssignedLabel assignedLabel = result.getModifications().getAssignedLabels(0);

        assertEquals("Empty node UUID", false, createdNode.getNodeUUID().isEmpty());
        assertEquals("Label has incorrect UUID", createdNode.getNodeUUID(), assignedLabel.getNodeUUID());
        assertEquals("Label has incorrect Name", "Label", assignedLabel.getName());
    }

    @Test
    public void testCreateNodeWithLabelWithProperty() {

        TransactionResponse result = sendQuery("CREATE (n:Label {property : 'test'})");

        assertEquals("Query should succeed", Status.SUCCESS, result.getResult());
        assertEquals("Query should not have errors", false, result.hasError());
        assertEquals("Query should have changes", true, result.hasModifications());

        assertEquals("Incorrect number of nodes created.", 1, result.getModifications().getCreatedNodesCount());
        assertEquals("Incorrect number of nodes deleted.", 0, result.getModifications().getDeletedNodesCount());
        assertEquals("Incorrect number of relationships created.", 0, result.getModifications().getCreatedRelationshipsCount());
        assertEquals("Incorrect number of relationships deleted.", 0, result.getModifications().getDeletedRelationshipsCount());
        assertEquals("Incorrect number of assigned relationship properties.", 0, result.getModifications().getAssignedRelationshipPropertiesCount());
        assertEquals("Incorrect number of removed relationship properties.", 0, result.getModifications().getRemovedRelationPropertiesCount());
        assertEquals("Incorrect number of assigned node properties.", 1, result.getModifications().getAssignedNodePropertiesCount());
        assertEquals("Incorrect number of removed node properties.", 0, result.getModifications().getRemovedNodePropertiesCount());
        assertEquals("Incorrect number of assigned labels.", 1, result.getModifications().getAssignedLabelsCount());
        assertEquals("Incorrect number of removed labels.", 0, result.getModifications().getRemovedLabelsCount());

        DatabaseModifications.CreatedNode createdNode = result.getModifications().getCreatedNodes(0);
        DatabaseModifications.AssignedLabel assignedLabel = result.getModifications().getAssignedLabels(0);
        DatabaseModifications.AssignedNodeProperty assignedNodeProperty = result.getModifications().getAssignedNodeProperties(0);

        assertEquals("Empty node UUID", false, createdNode.getNodeUUID().isEmpty());
        assertEquals("Label has incorrect UUID", createdNode.getNodeUUID(), assignedLabel.getNodeUUID());
        assertEquals("Label has incorrect Name", "Label", assignedLabel.getName());
        assertEquals("Property has incorrect UUID", createdNode.getNodeUUID(), assignedNodeProperty.getNodeUUID());
        assertEquals("Property has incorrect key", "property", assignedNodeProperty.getKey());
        assertEquals("Property has incorrect value", "test", assignedNodeProperty.getValue());
    }

    @Test
    public void testDeleteNodeNoRelation() {

        TransactionResponse result = sendQuery("MATCH (a:Person {" + Properties.UUID + ": '" + testUUIDs.get(PERSON_NO_RELATION) + "'}) DELETE a");

        assertEquals("Query should succeed", Status.SUCCESS, result.getResult());
        assertEquals("Query should not have errors", false, result.hasError());
        assertEquals("Query should have changes", true, result.hasModifications());

        assertEquals("Incorrect number of nodes created.", 0, result.getModifications().getCreatedNodesCount());
        assertEquals("Incorrect number of nodes deleted.", 1, result.getModifications().getDeletedNodesCount());
        assertEquals("Incorrect number of relationships created.", 0, result.getModifications().getCreatedRelationshipsCount());
        assertEquals("Incorrect number of relationships deleted.", 0, result.getModifications().getDeletedRelationshipsCount());
        assertEquals("Incorrect number of assigned relationship properties.", 0, result.getModifications().getAssignedRelationshipPropertiesCount());
        assertEquals("Incorrect number of removed relationship properties.", 0, result.getModifications().getRemovedRelationPropertiesCount());
        assertEquals("Incorrect number of assigned node properties.", 0, result.getModifications().getAssignedNodePropertiesCount());
        assertEquals("Incorrect number of removed node properties.", 1, result.getModifications().getRemovedNodePropertiesCount());
        assertEquals("Incorrect number of assigned labels.", 0, result.getModifications().getAssignedLabelsCount());
        assertEquals("Incorrect number of removed labels.", 1, result.getModifications().getRemovedLabelsCount());

        DatabaseModifications.DeletedNode deletedNode = result.getModifications().getDeletedNodes(0);
        DatabaseModifications.RemovedLabel removedLabel = result.getModifications().getRemovedLabels(0);
        DatabaseModifications.RemovedNodeProperty removedNodeProperty = result.getModifications().getRemovedNodeProperties(0);

        assertEquals("Empty node UUID", false, deletedNode.getNodeUUID().isEmpty());
        assertEquals("Label has incorrect UUID", deletedNode.getNodeUUID(), removedLabel.getNodeUUID());
        assertEquals("Label has incorrect Name", "Person", removedLabel.getName());

        assertEquals("Property has incorrect UUID", deletedNode.getNodeUUID(), removedNodeProperty.getNodeUUID());
        assertEquals("Property has incorrect key", "name", removedNodeProperty.getKey());
    }

    @Test
    public void testDeleteNodeWithRelation() {

        TransactionResponse result = sendQuery("MATCH (a:Person {" + Properties.UUID + ": '" + testUUIDs.get(PERSON_WITH_RELATION) + "'}) DELETE a");

        assertEquals("Query should succeed", Status.FAILURE, result.getResult());
        assertEquals("Query should have an error", true, result.hasError());
        assertEquals("Query incorrect error code", ErrorCode.CONSTRAINT_VIOLATION, result.getError().getCode());
        assertEquals("Query should not have changes", false, result.hasModifications());
    }

    @Test
    public void testAssignNodeLabel() {

        int[] testNodes = {NODE_NO_LABEL, PERSON_WITH_RELATION, PERSON_NO_RELATION};

        for (int node : testNodes) {
            TransactionResponse result = sendQuery("MATCH (n {" + Properties.UUID + ": '" + testUUIDs.get(node) + "'}) SET n:German");

            assertEquals("Query should succeed", Status.SUCCESS, result.getResult());
            assertEquals("Query should not have errors", false, result.hasError());
            assertEquals("Query should have changes", true, result.hasModifications());

            assertEquals("Incorrect number of nodes created.", 0, result.getModifications().getCreatedNodesCount());
            assertEquals("Incorrect number of nodes deleted.", 0, result.getModifications().getDeletedNodesCount());
            assertEquals("Incorrect number of relationships created.", 0, result.getModifications().getCreatedRelationshipsCount());
            assertEquals("Incorrect number of relationships deleted.", 0, result.getModifications().getDeletedRelationshipsCount());
            assertEquals("Incorrect number of assigned relationship properties.", 0, result.getModifications().getAssignedRelationshipPropertiesCount());
            assertEquals("Incorrect number of removed relationship properties.", 0, result.getModifications().getRemovedRelationPropertiesCount());
            assertEquals("Incorrect number of assigned node properties.", 0, result.getModifications().getAssignedNodePropertiesCount());
            assertEquals("Incorrect number of removed node properties.", 0, result.getModifications().getRemovedNodePropertiesCount());
            assertEquals("Incorrect number of assigned labels.", 1, result.getModifications().getAssignedLabelsCount());
            assertEquals("Incorrect number of removed labels.", 0, result.getModifications().getRemovedLabelsCount());

            DatabaseModifications.AssignedLabel assignedLabel = result.getModifications().getAssignedLabels(0);

            assertEquals("Empty node UUID", false, assignedLabel.getNodeUUID().isEmpty());
            assertEquals("Label has incorrect UUID", testUUIDs.get(node), assignedLabel.getNodeUUID());
            assertEquals("Label has incorrect Name", "German", assignedLabel.getName());
        }

    }

    @Test
    public void testRemoveNodeLabel() {

        int[] testNodes = {PERSON_WITH_RELATION, PERSON_NO_RELATION};

        for (int node : testNodes) {
            TransactionResponse result = sendQuery("MATCH (n {" + Properties.UUID + ": '" + testUUIDs.get(node) + "'}) Remove n:Person");

            assertEquals("Query should succeed", Status.SUCCESS, result.getResult());
            assertEquals("Query should not have errors", false, result.hasError());
            assertEquals("Query should have changes", true, result.hasModifications());

            assertEquals("Incorrect number of nodes created.", 0, result.getModifications().getCreatedNodesCount());
            assertEquals("Incorrect number of nodes deleted.", 0, result.getModifications().getDeletedNodesCount());
            assertEquals("Incorrect number of relationships created.", 0, result.getModifications().getCreatedRelationshipsCount());
            assertEquals("Incorrect number of relationships deleted.", 0, result.getModifications().getDeletedRelationshipsCount());
            assertEquals("Incorrect number of assigned relationship properties.", 0, result.getModifications().getAssignedRelationshipPropertiesCount());
            assertEquals("Incorrect number of removed relationship properties.", 0, result.getModifications().getRemovedRelationPropertiesCount());
            assertEquals("Incorrect number of assigned node properties.", 0, result.getModifications().getAssignedNodePropertiesCount());
            assertEquals("Incorrect number of removed node properties.", 0, result.getModifications().getRemovedNodePropertiesCount());
            assertEquals("Incorrect number of assigned labels.", 0, result.getModifications().getAssignedLabelsCount());
            assertEquals("Incorrect number of removed labels.", 1, result.getModifications().getRemovedLabelsCount());

            DatabaseModifications.RemovedLabel removedLabel = result.getModifications().getRemovedLabels(0);

            assertEquals("Empty node UUID", false, removedLabel.getNodeUUID().isEmpty());
            assertEquals("Label has incorrect UUID", testUUIDs.get(node), removedLabel.getNodeUUID());
            assertEquals("Label has incorrect name", "Person", removedLabel.getName());
        }
    }

    @Test
    public void testAssignNodeProperty() {

        int[] testNodes = {NODE_NO_LABEL, PERSON_WITH_RELATION, PERSON_NO_RELATION};

        for (int node : testNodes) {

            TransactionResponse result = sendQuery("MATCH (n {" + Properties.UUID + ": '" + testUUIDs.get(node) + "'}) SET n.newProperty = 'newValue' ");

            assertEquals("Query should succeed", Status.SUCCESS, result.getResult());
            assertEquals("Query should not have errors", false, result.hasError());
            assertEquals("Query should have changes", true, result.hasModifications());

            assertEquals("Incorrect number of nodes created.", 0, result.getModifications().getCreatedNodesCount());
            assertEquals("Incorrect number of nodes deleted.", 0, result.getModifications().getDeletedNodesCount());
            assertEquals("Incorrect number of relationships created.", 0, result.getModifications().getCreatedRelationshipsCount());
            assertEquals("Incorrect number of relationships deleted.", 0, result.getModifications().getDeletedRelationshipsCount());
            assertEquals("Incorrect number of assigned relationship properties.", 0, result.getModifications().getAssignedRelationshipPropertiesCount());
            assertEquals("Incorrect number of removed relationship properties.", 0, result.getModifications().getRemovedRelationPropertiesCount());
            assertEquals("Incorrect number of assigned node properties.", 1, result.getModifications().getAssignedNodePropertiesCount());
            assertEquals("Incorrect number of removed node properties.", 0, result.getModifications().getRemovedNodePropertiesCount());
            assertEquals("Incorrect number of assigned labels.", 0, result.getModifications().getAssignedLabelsCount());
            assertEquals("Incorrect number of removed labels.", 0, result.getModifications().getRemovedLabelsCount());

            DatabaseModifications.AssignedNodeProperty nodeProperty = result.getModifications().getAssignedNodeProperties(0);

            assertEquals("Empty node UUID", false, nodeProperty.getNodeUUID().isEmpty());
            assertEquals("Property has incorrect UUID", testUUIDs.get(node), nodeProperty.getNodeUUID());
            assertEquals("Property has incorrect key", "newProperty", nodeProperty.getKey());
            assertEquals("Property has incorrect value", "newValue", nodeProperty.getValue());
        }

    }

    @Test
    public void testRemoveNodeProperty() {

        int[] testNodes = {NODE_NO_LABEL, PERSON_WITH_RELATION, PERSON_NO_RELATION};

        for (int node : testNodes) {

            TransactionResponse result = sendQuery("MATCH (n {" + Properties.UUID + ": '" + testUUIDs.get(node) + "'}) Remove n.name ");

            assertEquals("Query should succeed", Status.SUCCESS, result.getResult());
            assertEquals("Query should not have errors", false, result.hasError());
            assertEquals("Query should have changes", true, result.hasModifications());

            assertEquals("Incorrect number of nodes created.", 0, result.getModifications().getCreatedNodesCount());
            assertEquals("Incorrect number of nodes deleted.", 0, result.getModifications().getDeletedNodesCount());
            assertEquals("Incorrect number of relationships created.", 0, result.getModifications().getCreatedRelationshipsCount());
            assertEquals("Incorrect number of relationships deleted.", 0, result.getModifications().getDeletedRelationshipsCount());
            assertEquals("Incorrect number of assigned relationship properties.", 0, result.getModifications().getAssignedRelationshipPropertiesCount());
            assertEquals("Incorrect number of removed relationship properties.", 0, result.getModifications().getRemovedRelationPropertiesCount());
            assertEquals("Incorrect number of assigned node properties.", 0, result.getModifications().getAssignedNodePropertiesCount());
            assertEquals("Incorrect number of removed node properties.", 1, result.getModifications().getRemovedNodePropertiesCount());
            assertEquals("Incorrect number of assigned labels.", 0, result.getModifications().getAssignedLabelsCount());
            assertEquals("Incorrect number of removed labels.", 0, result.getModifications().getRemovedLabelsCount());

            DatabaseModifications.RemovedNodeProperty removedNodeProperty = result.getModifications().getRemovedNodeProperties(0);

            assertEquals("Empty node UUID", false, removedNodeProperty.getNodeUUID().isEmpty());
            assertEquals("Property has incorrect UUID", testUUIDs.get(node), removedNodeProperty.getNodeUUID());
            assertEquals("Property has incorrect key", "name", removedNodeProperty.getKey());
        }

    }

    @Test
    public void testCreateRelationship() {

        int[] testNodes = {NODE_NO_LABEL, PERSON_NO_RELATION};

        for (int node : testNodes) {

            TransactionResponse result = sendQuery("MATCH (a), (b) " +
                    "WHERE a." + Properties.UUID + " = '" + testUUIDs.get(node) + "' AND b." + Properties.UUID + " = '" + testUUIDs.get(ADDRESS) + "'" +
                    "CREATE (a)-[r:LIVES_IN {rent:199, floor:'second'}]->(b)");

            assertEquals("Query should succeed", Status.SUCCESS, result.getResult());
            assertEquals("Query should not have errors", false, result.hasError());
            assertEquals("Query should have changes", true, result.hasModifications());

            assertEquals("Incorrect number of nodes created.", 0, result.getModifications().getCreatedNodesCount());
            assertEquals("Incorrect number of nodes deleted.", 0, result.getModifications().getDeletedNodesCount());
            assertEquals("Incorrect number of relationships created.", 1, result.getModifications().getCreatedRelationshipsCount());
            assertEquals("Incorrect number of relationships deleted.", 0, result.getModifications().getDeletedRelationshipsCount());
            assertEquals("Incorrect number of assigned relationship properties.", 2, result.getModifications().getAssignedRelationshipPropertiesCount());
            assertEquals("Incorrect number of removed relationship properties.", 0, result.getModifications().getRemovedRelationPropertiesCount());
            assertEquals("Incorrect number of assigned node properties.", 0, result.getModifications().getAssignedNodePropertiesCount());
            assertEquals("Incorrect number of removed node properties.", 0, result.getModifications().getRemovedNodePropertiesCount());
            assertEquals("Incorrect number of assigned labels.", 0, result.getModifications().getAssignedLabelsCount());
            assertEquals("Incorrect number of removed labels.", 0, result.getModifications().getRemovedLabelsCount());

            DatabaseModifications.CreatedRelationShip createdRelationship = result.getModifications().getCreatedRelationships(0);

            assertEquals("Empty relationship UUID", false, createdRelationship.getRelationshipUUID().isEmpty());
            assertEquals("Incorrect start nodeUUID", testUUIDs.get(node), createdRelationship.getStartNodeUUID());
            assertEquals("Incorrect end nodeUUID", testUUIDs.get(ADDRESS), createdRelationship.getEndNodeUUID());
            assertEquals("Incorrect end nodeUUID", "LIVES_IN", createdRelationship.getType());

            String relationshipUUID = createdRelationship.getRelationshipUUID();

            DatabaseModifications.AssignedRelationshipProperty property1 = result.getModifications().getAssignedRelationshipProperties(0);

            assertEquals("Empty node UUID", false, property1.getRelationshipUUID().isEmpty());
            assertEquals("Property has incorrect UUID", relationshipUUID, property1.getRelationshipUUID());
            assertEquals("Property has incorrect key", "floor", property1.getKey());
            assertEquals("Property has incorrect value", "second", property1.getValue());

            DatabaseModifications.AssignedRelationshipProperty property2 = result.getModifications().getAssignedRelationshipProperties(1);

            assertEquals("Empty node UUID", false, property2.getRelationshipUUID().isEmpty());
            assertEquals("Property has incorrect UUID", relationshipUUID, property2.getRelationshipUUID());
            assertEquals("Property has incorrect key", "rent", property2.getKey());
            assertEquals("Property has incorrect value", "199", property2.getValue());

        }
    }

    @Test
    public void testDeleteRelationship() {

        TransactionResponse result = sendQuery("MATCH ()-[r]-()" +
                "WHERE r." + Properties.UUID + " = '" + testUUIDs.get(LIVES_IN) + "' " +
                "DELETE r ");

        assertEquals("Query should succeed", Status.SUCCESS, result.getResult());
        assertEquals("Query should not have errors", false, result.hasError());
        assertEquals("Query should have changes", true, result.hasModifications());

        assertEquals("Incorrect number of nodes created.", 0, result.getModifications().getCreatedNodesCount());
        assertEquals("Incorrect number of nodes deleted.", 0, result.getModifications().getDeletedNodesCount());
        assertEquals("Incorrect number of relationships created.", 0, result.getModifications().getCreatedRelationshipsCount());
        assertEquals("Incorrect number of relationships deleted.", 1, result.getModifications().getDeletedRelationshipsCount());
        assertEquals("Incorrect number of assigned relationship properties.", 0, result.getModifications().getAssignedRelationshipPropertiesCount());
        assertEquals("Incorrect number of removed relationship properties.", 1, result.getModifications().getRemovedRelationPropertiesCount());
        assertEquals("Incorrect number of assigned node properties.", 0, result.getModifications().getAssignedNodePropertiesCount());
        assertEquals("Incorrect number of removed node properties.", 0, result.getModifications().getRemovedNodePropertiesCount());
        assertEquals("Incorrect number of assigned labels.", 0, result.getModifications().getAssignedLabelsCount());
        assertEquals("Incorrect number of removed labels.", 0, result.getModifications().getRemovedLabelsCount());

        DatabaseModifications.DeletedRelationship deletedRelationship = result.getModifications().getDeletedRelationships(0);

        assertEquals("Empty relationship UUID", false, deletedRelationship.getRelationshipUUID().isEmpty());
        assertEquals("Incorrect relationship UUID", testUUIDs.get(LIVES_IN), deletedRelationship.getRelationshipUUID());

        DatabaseModifications.RemovedRelationshipProperty removedRelationshipProperty = result.getModifications().getRemovedRelationProperties(0);

        assertEquals("Empty node UUID", false, removedRelationshipProperty.getRelationshipUUID().isEmpty());
        assertEquals("Property has incorrect UUID", testUUIDs.get(LIVES_IN), removedRelationshipProperty.getRelationshipUUID());
        assertEquals("Property has incorrect key", "rent", removedRelationshipProperty.getKey());
    }

    @Test
    public void testAssignRelationshipProperty() {

        TransactionResponse result = sendQuery("MATCH ()-[r]-()" +
                "WHERE r." + Properties.UUID + " = '" + testUUIDs.get(LIVES_IN) + "' " +
                "SET r.newProperty = 'newValue' ");

        assertEquals("Query should succeed", Status.SUCCESS, result.getResult());
        assertEquals("Query should not have errors", false, result.hasError());
        assertEquals("Query should have changes", true, result.hasModifications());

        assertEquals("Incorrect number of nodes created.", 0, result.getModifications().getCreatedNodesCount());
        assertEquals("Incorrect number of nodes deleted.", 0, result.getModifications().getDeletedNodesCount());
        assertEquals("Incorrect number of relationships created.", 0, result.getModifications().getCreatedRelationshipsCount());
        assertEquals("Incorrect number of relationships deleted.", 0, result.getModifications().getDeletedRelationshipsCount());
        assertEquals("Incorrect number of assigned relationship properties.", 1, result.getModifications().getAssignedRelationshipPropertiesCount());
        assertEquals("Incorrect number of removed relationship properties.", 0, result.getModifications().getRemovedRelationPropertiesCount());
        assertEquals("Incorrect number of assigned node properties.", 0, result.getModifications().getAssignedNodePropertiesCount());
        assertEquals("Incorrect number of removed node properties.", 0, result.getModifications().getRemovedNodePropertiesCount());
        assertEquals("Incorrect number of assigned labels.", 0, result.getModifications().getAssignedLabelsCount());
        assertEquals("Incorrect number of removed labels.", 0, result.getModifications().getRemovedLabelsCount());

        DatabaseModifications.AssignedRelationshipProperty property = result.getModifications().getAssignedRelationshipProperties(0);

        assertEquals("Empty node UUID", false, property.getRelationshipUUID().isEmpty());
        assertEquals("Property has incorrect UUID", testUUIDs.get(LIVES_IN), property.getRelationshipUUID());
        assertEquals("Property has incorrect key", "newProperty", property.getKey());
        assertEquals("Property has incorrect value", "newValue", property.getValue());

    }

    @Test
    public void testRemoveRelationshipProperty() {

        TransactionResponse result = sendQuery("MATCH ()-[r]-()" +
                "WHERE r." + Properties.UUID + " = '" + testUUIDs.get(LIVES_IN) + "' " +
                "Remove r.rent ");

        assertEquals("Query should succeed", Status.SUCCESS, result.getResult());
        assertEquals("Query should not have errors", false, result.hasError());
        assertEquals("Query should have changes", true, result.hasModifications());

        assertEquals("Incorrect number of nodes created.", 0, result.getModifications().getCreatedNodesCount());
        assertEquals("Incorrect number of nodes deleted.", 0, result.getModifications().getDeletedNodesCount());
        assertEquals("Incorrect number of relationships created.", 0, result.getModifications().getCreatedRelationshipsCount());
        assertEquals("Incorrect number of relationships deleted.", 0, result.getModifications().getDeletedRelationshipsCount());
        assertEquals("Incorrect number of assigned relationship properties.", 0, result.getModifications().getAssignedRelationshipPropertiesCount());
        assertEquals("Incorrect number of removed relationship properties.", 1, result.getModifications().getRemovedRelationPropertiesCount());
        assertEquals("Incorrect number of assigned node properties.", 0, result.getModifications().getAssignedNodePropertiesCount());
        assertEquals("Incorrect number of removed node properties.", 0, result.getModifications().getRemovedNodePropertiesCount());
        assertEquals("Incorrect number of assigned labels.", 0, result.getModifications().getAssignedLabelsCount());
        assertEquals("Incorrect number of removed labels.", 0, result.getModifications().getRemovedLabelsCount());

        DatabaseModifications.RemovedRelationshipProperty property = result.getModifications().getRemovedRelationProperties(0);

        assertEquals("Empty node UUID", false, property.getRelationshipUUID().isEmpty());
        assertEquals("Property has incorrect UUID", testUUIDs.get(LIVES_IN), property.getRelationshipUUID());
        assertEquals("Property has incorrect key", "rent", property.getKey());
    }

    @Test
    public void testInvalidQueryTransaction() {

        TransactionResponse response = sendQuery("FakeQuery");

        assertEquals("Incorrect query result.", Status.FAILURE, response.getResult());
        assertFalse("No modifications expected", response.hasModifications());
    }


    private void insertTestData() {

        TransactionResponse transactionResponse = sendQuery("CREATE (n:Person {name :'John'})");
        String personNoRelationUUID = transactionResponse.getModifications().getCreatedNodes(0).getNodeUUID();

        transactionResponse = sendQuery("CREATE (n {name :'Peter'})");
        String nodeNoLabel = transactionResponse.getModifications().getCreatedNodes(0).getNodeUUID();

        transactionResponse = sendQuery("CREATE (n:Person {name :'William', surname : 'Clinton' })");
        String personWithRelationUUID = transactionResponse.getModifications().getCreatedNodes(0).getNodeUUID();

        transactionResponse = sendQuery("CREATE (n:Address {street : 'Lombard Street', city : 'San Fransisco', country : 'United States' })");
        String addressUUID = transactionResponse.getModifications().getCreatedNodes(0).getNodeUUID();

        transactionResponse = sendQuery("MATCH (a:Person), (b:Address) " +
                "WHERE a." + Properties.UUID + " = '" + personWithRelationUUID + "' AND b." + Properties.UUID + " = '" + addressUUID + "'" +
                "CREATE (a)-[r:LIVES_IN {rent:500}]->(b) ");
        String livesInUUID = transactionResponse.getModifications().getCreatedRelationships(0).getRelationshipUUID();

        testUUIDs.clear();
        testUUIDs.put(NODE_NO_LABEL, nodeNoLabel);
        testUUIDs.put(PERSON_NO_RELATION, personNoRelationUUID);
        testUUIDs.put(PERSON_WITH_RELATION, personWithRelationUUID);
        testUUIDs.put(ADDRESS, addressUUID);
        testUUIDs.put(LIVES_IN, livesInUUID);
    }

    private TransactionResponse sendQuery(String query) {

        TransactionRequest transactionRequest = TransactionRequest.newBuilder().setTransactionId(getTranscationID()).addQueries(query).build();

        // Execute
        BlockExecuteRequest executeRequest = BlockExecuteRequest.newBuilder()
                .setBlockId(getBlockID())
                .addTransactions(transactionRequest)
                .build();
        BlockExecuteResponse executeResponse = blockingStub.executeBlock(executeRequest);

        assertTrue("Block execution failed", executeResponse.getSuccess());

        // Retrieve
        BlockChangesRequest changesRequest = BlockChangesRequest.newBuilder().setBlockId(getBlockID()).build();
        BlockChangesResponse changeResponse = blockingStub.retrieveBlockChanges(changesRequest);

        assertEquals("Received incorrect block id", getBlockID(), changeResponse.getBlockId());
        assertEquals("Received incorrect amount of transactions.", 1, changeResponse.getTransactionsCount());

        TransactionResponse transactionResponse = changeResponse.getTransactions(0);

        // Delete
        DeleteBlockRequest deleteRequest = DeleteBlockRequest.newBuilder().setBlockId(getBlockID()).build();
        DeleteBlockResponse deleteResponse = blockingStub.deleteBlockChanges(deleteRequest);

        assertTrue("Could not delete block", deleteResponse.getSuccess());

        return transactionResponse;
    }

    private String getBlockID() {
        return "block_id" + block_id;
    }

    private String getTranscationID() {
        tx_id++;
        return "tx_id" + tx_id;
    }
}

