package com.bitfury.neo4j.transaction_manager;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;


public class TransactionManagerTest {

    private GraphDatabaseService db;
    private static int block_id = 0;

    static final int TEST_GRPC_PORT = Properties.GRPC_DEFAULT_PORT;

    private ManagedChannel channel;
    private static TransactionManagerGrpc.TransactionManagerBlockingStub blockingStub;

    @Before
    public void setUp() {
        db = new TestGraphDatabaseFactory()
                .newImpermanentDatabaseBuilder()
                .setConfig(Properties.GRPC_KEY_PORT, String.valueOf(TEST_GRPC_PORT))
                .newGraphDatabase();

        channel = ManagedChannelBuilder.forAddress("localhost", TEST_GRPC_PORT)
                .usePlaintext(true)
                .build();

        blockingStub = TransactionManagerGrpc.newBlockingStub(channel);

        block_id++;
    }

    @After
    public void tearDown() {
        channel.shutdown();
        db.shutdown();
    }

    @Test
    public void testNonExistingBlockDelete() {

        DeleteBlockRequest deleteRequest = DeleteBlockRequest.newBuilder().setBlockId(getBlockID()).build();
        DeleteBlockResponse deleteResponse = blockingStub.deleteBlockChanges(deleteRequest);

        assertFalse(deleteResponse.getSuccess());
    }

    @Test
    public void testDoubleDelete() {

        // Execute
        BlockExecuteRequest executeRequest = BlockExecuteRequest.newBuilder().setBlockId(getBlockID()).build();
        BlockExecuteResponse executeResponse = blockingStub.executeBlock(executeRequest);

        assertTrue(executeResponse.getSuccess());

        // Delete
        DeleteBlockRequest deleteRequest = DeleteBlockRequest.newBuilder().setBlockId(getBlockID()).build();
        DeleteBlockResponse deleteResponse = blockingStub.deleteBlockChanges(deleteRequest);

        assertTrue(deleteResponse.getSuccess());

        // Delete twice
        deleteResponse = blockingStub.deleteBlockChanges(deleteRequest);

        assertFalse(deleteResponse.getSuccess());

    }

    @Test
    public void testEmptyBlock() {

        // Execute
        BlockExecuteRequest executeRequest = BlockExecuteRequest.newBuilder().setBlockId(getBlockID()).build();
        BlockExecuteResponse executeResponse = blockingStub.executeBlock(executeRequest);

        assertTrue(executeResponse.getSuccess());

        // Retrieve
        BlockChangesRequest changesRequest = BlockChangesRequest.newBuilder().setBlockId(getBlockID()).build();
        BlockChangesResponse changeResponse = blockingStub.retrieveBlockChanges(changesRequest);

        assertEquals(changeResponse.getBlockId(), getBlockID());
        assertTrue(changeResponse.getTransactionsCount() == 0);

        // Delete
        DeleteBlockRequest deleteRequest = DeleteBlockRequest.newBuilder().setBlockId(getBlockID()).build();
        DeleteBlockResponse deleteResponse = blockingStub.deleteBlockChanges(deleteRequest);

        assertTrue(deleteResponse.getSuccess());
    }

    @Test(expected = io.grpc.StatusRuntimeException.class)
    public void testNonExistingBlockRetrieve() {

        BlockChangesRequest changesRequest = BlockChangesRequest.newBuilder().setBlockId(getBlockID()).build();
        blockingStub.retrieveBlockChanges(changesRequest);

    }

    @Test
    public void testDoubleBlockRetrieve() {

        // Execute
        TransactionRequest transaction = TransactionRequest.newBuilder().setTransactionId("txID1").addQueries("CREATE (n:Person {name:'Sjoerd'})").build();
        BlockExecuteRequest executeRequest = BlockExecuteRequest.newBuilder().setBlockId(getBlockID()).addTransactions(transaction).build();
        BlockExecuteResponse executeResponse = blockingStub.executeBlock(executeRequest);

        assertTrue(executeResponse.getSuccess());

        // Retrieve
        BlockChangesRequest changesRequest = BlockChangesRequest.newBuilder().setBlockId(getBlockID()).build();
        BlockChangesResponse changeResponse = blockingStub.retrieveBlockChanges(changesRequest);

        assertEquals(changeResponse.getBlockId(), getBlockID());
        assertTrue(changeResponse.getTransactionsCount() > 0);

        // Second retrieve
        BlockChangesResponse secondChangeResponse = blockingStub.retrieveBlockChanges(changesRequest);

        assertEquals(changeResponse.getBlockId(), getBlockID());
        assertTrue(secondChangeResponse.getTransactionsCount() > 0);

        assertEquals(changeResponse, secondChangeResponse);
    }

    @Test(expected = io.grpc.StatusRuntimeException.class)
    public void testTransactionWithoutID() {

        // Execute
        TransactionRequest transactionRequest = TransactionRequest.newBuilder()
                .addQueries("CREATE (n:Person { name: 'Sjoerd', title: 'Developer' })")
                .build();
        BlockExecuteRequest executeRequest = BlockExecuteRequest.newBuilder()
                .addTransactions(transactionRequest)
                .setBlockId(getBlockID())
                .build();
        BlockExecuteResponse executeResponse = blockingStub.executeBlock(executeRequest);
    }

    @Test
    public void testSingleTransactionSingleQuery() {

        // Execute
        TransactionRequest transactionRequest = TransactionRequest.newBuilder()
                .addQueries("CREATE (n:Person { name: 'Sjoerd' })")
                .setTransactionId("txID2")
                .build();
        BlockExecuteRequest executeRequest = BlockExecuteRequest.newBuilder()
                .addTransactions(transactionRequest)
                .setBlockId(getBlockID())
                .build();
        BlockExecuteResponse executeResponse = blockingStub.executeBlock(executeRequest);

        assertTrue("Execute block failed.",executeResponse.getSuccess());

        // Retrieve
        BlockChangesRequest changesRequest = BlockChangesRequest.newBuilder()
                .setBlockId(getBlockID())
                .build();
        BlockChangesResponse changeResponse = blockingStub.retrieveBlockChanges(changesRequest);

        assertEquals("Received incorrect block ID.", getBlockID(), changeResponse.getBlockId());
        assertEquals("Received incorrect number of transactions.", 1, changeResponse.getTransactionsCount());

        TransactionResponse transactionResponse = changeResponse.getTransactions(0);

        assertEquals("Transaction execution failed", Status.SUCCESS, transactionResponse.getResult());
        assertEquals("Received incorrect transaction ID", transactionRequest.getTransactionId(), transactionResponse.getTransactionId());
        assertTrue("Transaction did not have modifications", transactionResponse.hasModifications());

        // Compare changes
        assertEquals("Incorrect number of nodes created.", 1, transactionResponse.getModifications().getCreatedNodesCount());
        assertEquals("Incorrect number of nodes deleted.", 0, transactionResponse.getModifications().getDeletedNodesCount());
        assertEquals("Incorrect number of relationships created.", 0, transactionResponse.getModifications().getCreatedRelationshipsCount());
        assertEquals("Incorrect number of relationships deleted.", 0, transactionResponse.getModifications().getDeletedRelationshipsCount());
        assertEquals("Incorrect number of assigned relationship properties.", 0, transactionResponse.getModifications().getAssignedRelationshipPropertiesCount());
        assertEquals("Incorrect number of removed relationship properties.", 0, transactionResponse.getModifications().getRemovedRelationPropertiesCount());
        assertEquals("Incorrect number of assigned node properties.", 1, transactionResponse.getModifications().getAssignedNodePropertiesCount());
        assertEquals("Incorrect number of removed node properties.", 0,transactionResponse.getModifications().getRemovedNodePropertiesCount());
        assertEquals("Incorrect number of assigned labels.", 1, transactionResponse.getModifications().getAssignedLabelsCount());
        assertEquals("Incorrect number of removed labels.", 0,transactionResponse.getModifications().getRemovedLabelsCount());

        // First query
        DatabaseModifications.CreatedNode createdNode = transactionResponse.getModifications().getCreatedNodes(0);
        String nodeUUID = createdNode.getNodeUUID();
        assertFalse("Empty UUID", nodeUUID.isEmpty());
        DatabaseModifications.AssignedNodeProperty nodeProperty = transactionResponse.getModifications().getAssignedNodeProperties(0);
        assertEquals("Incorrect property UUID", nodeUUID, nodeProperty.getNodeUUID());
        assertEquals("Incorrect property key", "name", nodeProperty.getKey());
        assertEquals("Incorrect property key", "Sjoerd", nodeProperty.getValue());
        DatabaseModifications.AssignedLabel label = transactionResponse.getModifications().getAssignedLabels(0);
        assertEquals("Label name incorrect", "Person", label.getName());
        assertEquals("Label uuid", nodeUUID, label.getNodeUUID());
    }

    @Test
    public void testSingleTransactionMultipleQueries() {

        // Execute
        TransactionRequest transactionRequest = TransactionRequest.newBuilder()
                .addQueries("CREATE (n:Person { name: 'Sjoerd' })")
                .addQueries("CREATE (n:Person { name: 'Sjoerd' })")
                .addQueries("CREATE (n:Person { name: 'Sjoerd' })")
                .setTransactionId("txID2")
                .build();
        BlockExecuteRequest executeRequest = BlockExecuteRequest.newBuilder()
                .addTransactions(transactionRequest)
                .setBlockId(getBlockID())
                .build();
        BlockExecuteResponse executeResponse = blockingStub.executeBlock(executeRequest);

        assertTrue(executeResponse.getSuccess());

        // Retrieve
        BlockChangesRequest changesRequest = BlockChangesRequest.newBuilder()
                .setBlockId(getBlockID())
                .build();
        BlockChangesResponse changeResponse = blockingStub.retrieveBlockChanges(changesRequest);

        assertEquals("Received block  has incorrect block id", getBlockID(), changeResponse.getBlockId());
        assertEquals("Received block has  incorrect transaction count", 1, changeResponse.getTransactionsCount());

        TransactionResponse transactionResponse = changeResponse.getTransactions(0);

        assertEquals(transactionResponse.getResult(), Status.SUCCESS);
        assertEquals(transactionResponse.getTransactionId(), transactionRequest.getTransactionId());
        assertTrue(transactionResponse.hasModifications());

        // Compare changes
        assertEquals("Incorrect number of nodes created.", 3, transactionResponse.getModifications().getCreatedNodesCount());
        assertEquals("Incorrect number of nodes deleted.", 0, transactionResponse.getModifications().getDeletedNodesCount());
        assertEquals("Incorrect number of relationships created.", 0, transactionResponse.getModifications().getCreatedRelationshipsCount());
        assertEquals("Incorrect number of relationships deleted.", 0, transactionResponse.getModifications().getDeletedRelationshipsCount());
        assertEquals("Incorrect number of assigned relationship properties.", 0, transactionResponse.getModifications().getAssignedRelationshipPropertiesCount());
        assertEquals("Incorrect number of removed relationship properties.", 0, transactionResponse.getModifications().getRemovedRelationPropertiesCount());
        assertEquals("Incorrect number of assigned node properties.", 3, transactionResponse.getModifications().getAssignedNodePropertiesCount());
        assertEquals("Incorrect number of removed node properties.", 0,transactionResponse.getModifications().getRemovedNodePropertiesCount());
        assertEquals("Incorrect number of assigned labels.", 3, transactionResponse.getModifications().getAssignedLabelsCount());
        assertEquals("Incorrect number of removed labels.", 0,transactionResponse.getModifications().getRemovedLabelsCount());

        DatabaseModifications.CreatedNode createdNode1 = transactionResponse.getModifications().getCreatedNodes(0);
        String nodeUUID1 = createdNode1.getNodeUUID();
        assertFalse("Empty UUID", nodeUUID1.isEmpty());

        assertTrue("Could not find created node property in modification list.", transactionResponse
                .getModifications()
                .getAssignedNodePropertiesList()
                .stream()
                .anyMatch(item -> nodeUUID1.equals(item.getNodeUUID()) && "name".equals(item.getKey()) && !item.getValue().isEmpty()));

        assertTrue("Could not find created node label in modification list.", transactionResponse
                .getModifications()
                .getAssignedLabelsList()
                .stream()
                .anyMatch(item -> nodeUUID1.equals(item.getNodeUUID()) && "Person".equals(item.getName())));

        DatabaseModifications.CreatedNode createdNode2 = transactionResponse.getModifications().getCreatedNodes(1);
        String nodeUUID2 = createdNode2.getNodeUUID();
        assertFalse("Empty UUID", nodeUUID2.isEmpty());

        assertTrue("Could not find created node property in modification list.", transactionResponse
                .getModifications()
                .getAssignedNodePropertiesList()
                .stream()
                .anyMatch(item -> nodeUUID2.equals(item.getNodeUUID()) && "name".equals(item.getKey()) && !item.getValue().isEmpty()));

        assertTrue("Could not find created node label in modification list.", transactionResponse
                .getModifications()
                .getAssignedLabelsList()
                .stream()
                .anyMatch(item -> nodeUUID2.equals(item.getNodeUUID()) && "Person".equals(item.getName())));

        DatabaseModifications.CreatedNode createdNode3 = transactionResponse.getModifications().getCreatedNodes(2);
        String nodeUUID3 = createdNode3.getNodeUUID();
        assertFalse("Empty UUID", nodeUUID3.isEmpty());

        assertTrue("Could not find created node property in modification list.", transactionResponse
                .getModifications()
                .getAssignedNodePropertiesList()
                .stream()
                .anyMatch(item -> nodeUUID3.equals(item.getNodeUUID()) && "name".equals(item.getKey()) && !item.getValue().isEmpty()));

        assertTrue("Could not find created node label in modification list.", transactionResponse
                .getModifications()
                .getAssignedLabelsList()
                .stream()
                .anyMatch(item -> nodeUUID3.equals(item.getNodeUUID()) && "Person".equals(item.getName())));
    }

    @Test
    public void testSingleTransactionMultipleQueriesEmptyResult() {

        // Execute
        TransactionRequest transactionRequest = TransactionRequest.newBuilder()
                .addQueries("CREATE (n:Person { name: 'Sjoerd' })")
                .addQueries("MATCH (n:Person { name: 'Sjoerd' }) DELETE n")
                .setTransactionId("txID2")
                .build();
        BlockExecuteRequest executeRequest = BlockExecuteRequest.newBuilder()
                .addTransactions(transactionRequest)
                .setBlockId(getBlockID())
                .build();
        BlockExecuteResponse executeResponse = blockingStub.executeBlock(executeRequest);

        assertTrue(executeResponse.getSuccess());

        // Retrieve
        BlockChangesRequest changesRequest = BlockChangesRequest.newBuilder()
                .setBlockId(getBlockID())
                .build();
        BlockChangesResponse changeResponse = blockingStub.retrieveBlockChanges(changesRequest);

        assertEquals("Received block has incorrect block id", getBlockID(), changeResponse.getBlockId());
        assertEquals("Received block has incorrect transaction count", 1, changeResponse.getTransactionsCount());

        TransactionResponse transactionResponse = changeResponse.getTransactions(0);

        assertEquals(transactionResponse.getResult(), Status.SUCCESS);
        assertEquals(transactionResponse.getTransactionId(), transactionRequest.getTransactionId());
        assertTrue(transactionResponse.hasModifications());

        // Compare changes
        assertEquals("Incorrect number of nodes created.", 0, transactionResponse.getModifications().getCreatedNodesCount());
        assertEquals("Incorrect number of nodes deleted.", 0, transactionResponse.getModifications().getDeletedNodesCount());
        assertEquals("Incorrect number of relationships created.", 0, transactionResponse.getModifications().getCreatedRelationshipsCount());
        assertEquals("Incorrect number of relationships deleted.", 0, transactionResponse.getModifications().getDeletedRelationshipsCount());
        assertEquals("Incorrect number of assigned relationship properties.", 0, transactionResponse.getModifications().getAssignedRelationshipPropertiesCount());
        assertEquals("Incorrect number of removed relationship properties.", 0, transactionResponse.getModifications().getRemovedRelationPropertiesCount());
        assertEquals("Incorrect number of assigned node properties.", 0, transactionResponse.getModifications().getAssignedNodePropertiesCount());
        assertEquals("Incorrect number of removed node properties.", 0,transactionResponse.getModifications().getRemovedNodePropertiesCount());
        assertEquals("Incorrect number of assigned labels.", 0, transactionResponse.getModifications().getAssignedLabelsCount());
        assertEquals("Incorrect number of removed labels.", 0,transactionResponse.getModifications().getRemovedLabelsCount());
    }

    @Test
    public void testMultipleTransactionsMultipleQueries() {

        int nr_transactions = 50;
        int nr_queries = 50;

        BlockExecuteRequest.Builder bkBuilder = BlockExecuteRequest.newBuilder();
        bkBuilder.setBlockId(getBlockID());

        for (int i = 0; i < nr_transactions; i++) {

            TransactionRequest.Builder txBuilder = TransactionRequest.newBuilder();
            txBuilder.setTransactionId("txID" + i);

            for (int j = 0; j < nr_queries; j++) {
                txBuilder.addQueries("CREATE (n:Person { name: 'Sjoerd_" + i + "_" + j + "' })");
            }

            bkBuilder.addTransactions(txBuilder);
        }

        // Execute
        BlockExecuteResponse executeResponse = blockingStub.executeBlock(bkBuilder.build());
        assertTrue(executeResponse.getSuccess());

        // Retrieve
        BlockChangesRequest changesRequest = BlockChangesRequest.newBuilder()
                .setBlockId(getBlockID())
                .build();
        BlockChangesResponse changeResponse = blockingStub.retrieveBlockChanges(changesRequest);

        assertEquals("Received block has incorrect block id", getBlockID(), changeResponse.getBlockId());
        assertEquals("Received block has incorrect transaction count", nr_transactions, changeResponse.getTransactionsCount());

        // Compare changes
        for (int i = 0; i < nr_transactions; i++) {

            TransactionResponse transactionResponse = changeResponse.getTransactions(i);

            assertEquals("Incorrect number of nodes created.", nr_queries, transactionResponse.getModifications().getCreatedNodesCount());
            assertEquals("Incorrect number of nodes deleted.", 0, transactionResponse.getModifications().getDeletedNodesCount());
            assertEquals("Incorrect number of relationships created.", 0, transactionResponse.getModifications().getCreatedRelationshipsCount());
            assertEquals("Incorrect number of relationships deleted.", 0, transactionResponse.getModifications().getDeletedRelationshipsCount());
            assertEquals("Incorrect number of assigned relationship properties.", 0, transactionResponse.getModifications().getAssignedRelationshipPropertiesCount());
            assertEquals("Incorrect number of removed relationship properties.", 0, transactionResponse.getModifications().getRemovedRelationPropertiesCount());
            assertEquals("Incorrect number of assigned node properties.", nr_queries, transactionResponse.getModifications().getAssignedNodePropertiesCount());
            assertEquals("Incorrect number of removed node properties.", 0,transactionResponse.getModifications().getRemovedNodePropertiesCount());
            assertEquals("Incorrect number of assigned labels.", nr_queries, transactionResponse.getModifications().getAssignedLabelsCount());
            assertEquals("Incorrect number of removed labels.", 0,transactionResponse.getModifications().getRemovedLabelsCount());

            // Check if for every node, the related label and name property is in the modification list.
            for (int j = 0; j < nr_queries; j++) {

                DatabaseModifications.CreatedNode createdNode = transactionResponse.getModifications().getCreatedNodes(j);
                String nodeUUID = createdNode.getNodeUUID();
                assertFalse("Empty UUID", nodeUUID.isEmpty());

                // New created node property exists
                assertTrue(transactionResponse
                        .getModifications()
                        .getAssignedNodePropertiesList()
                        .stream()
                        .anyMatch(item -> nodeUUID.equals(item.getNodeUUID()) && "name".equals(item.getKey())));

                // New created label exists
                assertTrue(transactionResponse
                        .getModifications()
                        .getAssignedLabelsList()
                        .stream()
                        .anyMatch(item -> nodeUUID.equals(item.getNodeUUID()) && "Person".equals(item.getName())));

            }
        }
    }

    private String getBlockID() {
        return "block_id" + block_id;
    }
}

