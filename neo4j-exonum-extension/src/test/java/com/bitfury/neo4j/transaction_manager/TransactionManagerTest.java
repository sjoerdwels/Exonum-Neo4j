package com.bitfury.neo4j.transaction_manager;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;

import java.util.Random;
import java.util.stream.Collectors;

public class TransactionManagerTest {

    static final String TEST_PREFIX = "myPrefix";
    private ManagedChannel channel;

    @Rule
    public final Neo4jRule neo4j = new Neo4jRule();

    private static TransactionManagerGrpc.TransactionManagerBlockingStub blockingStub;

    @Before
    public void setup() {
        channel = ManagedChannelBuilder.forAddress("localhost", 9994)
                .usePlaintext(true)
                .build();
        blockingStub = TransactionManagerGrpc.newBlockingStub(channel);
    }

    @After
    public  void after()  {
        channel.shutdown();
    }

    @Test
    public void testverify() {

        TransactionRequest request = TransactionRequest.newBuilder()
                .addQueries("CREATE (n:Person { name: 'Sjoerd', title: 'Developer' })")
                .setUUIDPrefix(TEST_PREFIX)
                .build();
        TransactionResponse response = blockingStub.verify(request);
        assert (response.getResult() == Status.SUCCESS);
    }

    @Test
    public void testexecute() {

        TransactionRequest request = TransactionRequest.newBuilder()
                .addQueries("CREATE (n:Person { name: 'Sjoerd', title: 'Developer' })")
                .setUUIDPrefix(TEST_PREFIX)
                .build();
        TransactionResponse response = blockingStub.execute(request);
        assert (response.getResult() == Status.SUCCESS);
    }

    @Test
    public void testverifyWithoutQueries() {

        TransactionRequest request = TransactionRequest.newBuilder()
                .setUUIDPrefix(TEST_PREFIX)
                .build();

        TransactionResponse response = blockingStub.verify(request);
        assert (response.getResult() == Status.FAILURE);
    }

    @Test
    public void testTransactionWithoutUUIDPrefix() {

        TransactionRequest request = TransactionRequest.newBuilder()
                .addQueries("CREATE (n:Person { name: 'Sjoerd', title: 'Developer' })")
                .build();

        TransactionResponse response = blockingStub.execute(request);
        assert (response.getResult() == Status.FAILURE);

    }

    @Test
    public void testSingleNodeSuccessTransaction() {

        TransactionRequest request = TransactionRequest.newBuilder()
                .setUUIDPrefix(TEST_PREFIX)
                .addQueries("CREATE (n:Person { name: 'Sjoerd', title: 'Developer' })")
                .build();
        TransactionResponse response = blockingStub.execute(request);

        assert (response.getResult() == Status.SUCCESS);
        assert (response.getModifications().getCreatedNodesCount()==1);
        assert (response.getModifications().getAssignedNodePropertiesCount()==2);
        assert (response.getModifications().getAssignedLabelsCount()==1);

        request = TransactionRequest.newBuilder()
                .setUUIDPrefix(TEST_PREFIX + "2")
                .addQueries("MATCH (n:Person) SET n.name =  'peter'")
                .build();
        response = blockingStub.execute(request);

        assert (response.getResult() == Status.SUCCESS);
        assert (response.getModifications().getAssignedNodePropertiesList().get(0).getValue().equals("peter"));
        assert (response.getModifications().getAssignedNodePropertiesCount()==1);
        assert (response.getModifications().getRemovedNodePropertiesCount()==0);

        request = TransactionRequest.newBuilder()
                .setUUIDPrefix(TEST_PREFIX + "3")
                .addQueries("MATCH (n:Person) DELETE n")
                .build();
        response = blockingStub.execute(request);

        assert (response.getResult() == Status.SUCCESS);
        assert (response.getModifications().getDeletedNodes(0).getNodeUUID().equals("myPrefix_0"));
        assert (response.getModifications().getDeletedNodesCount()==1);
        assert (response.getModifications().getRemovedNodePropertiesCount()==3);
        assert (response.getModifications().getRemovedLabelsCount()==1);

    }

    @Test
    public void testSingleUUIDAllocation() {

        TransactionRequest request = TransactionRequest.newBuilder()
                .addQueries("CREATE (n:Person { name:\"Silver\"})")
                .setUUIDPrefix(TEST_PREFIX)
                .build();
        TransactionResponse response = blockingStub.execute(request);

        assert  (response.getModifications().getCreatedNodes(0).getNodeUUID().equals("myPrefix_0"));
        assert  (response.getModifications().getAssignedNodePropertiesList().stream().filter(prop -> prop.getKey().equals(Properties.UUID)).collect(Collectors.toList()).size()==0);
        assert (response.getResult() == Status.SUCCESS);

        request = TransactionRequest.newBuilder()
                .addQueries("MATCH (n:Person) WHERE n.name=\"Silver\"  DELETE n")
                .setUUIDPrefix(TEST_PREFIX + "2")
                .build();
        response = blockingStub.execute(request);

        assert  (response.getModifications().getDeletedNodes(0).getNodeUUID().equals("myPrefix_0"));
        assert  (response.getModifications().getRemovedNodePropertiesList().stream().filter(prop -> prop.getKey().equals(Properties.UUID)).collect(Collectors.toList()).size()==1);
        assert (response.getResult() == Status.SUCCESS);
    }

    @Test
    public void testMultipleUUIDAllocation(){
        TransactionRequest request = TransactionRequest.newBuilder()
                .addQueries("CREATE (n:Person {title: 'Tester' })")
                .addQueries("CREATE (n:Person {title: 'Manager' })")
                .setUUIDPrefix(TEST_PREFIX + "2")
                .build();
        TransactionResponse response = blockingStub.execute(request);

        assert  (response.getModifications().getCreatedNodes(0).getNodeUUID().equals("myPrefix2_0"));
        assert  (response.getModifications().getCreatedNodes(1).getNodeUUID().equals("myPrefix2_1"));
        assert  (response.getModifications().getAssignedNodePropertiesList().stream().filter(prop -> prop.getKey().equals(Properties.UUID)).collect(Collectors.toList()).size()==0);
        assert (response.getResult() == Status.SUCCESS);

        request = TransactionRequest.newBuilder()
                .addQueries("MATCH (n:Person) WHERE n.title='Tester'  DELETE n")
                .addQueries("MATCH (n:Person) WHERE n.title='Manager'  DELETE n")
                .setUUIDPrefix(TEST_PREFIX + "2")
                .build();
        response = blockingStub.execute(request);

        assert  (response.getModifications().getDeletedNodes(0).getNodeUUID().equals("myPrefix2_0"));
        assert  (response.getModifications().getDeletedNodes(1).getNodeUUID().equals("myPrefix2_1"));
        assert  (response.getModifications().getRemovedNodePropertiesList().stream().filter(prop -> prop.getKey().equals(Properties.UUID)).collect(Collectors.toList()).size()==2);
        assert (response.getResult() == Status.SUCCESS);
    }

   @Test
    public void testModifyUUIDTransaction() {

        TransactionRequest request = TransactionRequest.newBuilder()
                .setUUIDPrefix(TEST_PREFIX)
                .addQueries("CREATE (n:Person { id: '2', title: 'Developer' })")
                .build();
        TransactionResponse response = blockingStub.execute(request);

        assert (response.getResult() == Status.SUCCESS);

        request = TransactionRequest.newBuilder()
                .setUUIDPrefix(TEST_PREFIX + "2")
                .addQueries("MATCH (n:Person) SET n."+ Properties.UUID + " =  'modifiedUUID'")
                .build();
        response = blockingStub.execute(request);

        assert (response.getResult() == Status.FAILURE);
    }

    @Test
    public void testInvalidQueryTransaction() {

        TransactionRequest request = TransactionRequest.newBuilder()
                .setUUIDPrefix(TEST_PREFIX)
                .addQueries("FakeQuery")
                .build();
        TransactionResponse response = blockingStub.execute(request);

        assert (response.getResult() == Status.FAILURE);
    }


    public void testMultipleThreadsTransaction() {

        int number = 100;

        Thread transactionThreads[] = new Thread[number];

        for (int j = 0; j < number; j++) {
            transactionThreads[j] = new Thread(() -> {

                Random random = new Random();
                try {
                    Thread.sleep(random.nextInt(500));
                } catch (Exception ex) {
                }

                TransactionRequest request = TransactionRequest.newBuilder().setUUIDPrefix(TEST_PREFIX).addQueries(" CREATE (n)").build();
                TransactionResponse response = blockingStub.verify(request);

                assert (response.getResult() == Status.SUCCESS);

            });
            transactionThreads[j].start();
        }
        try {
            for (int j = 0; j < number; j++) {
                transactionThreads[j].join();
            }
        } catch (Exception e) {
            System.out.println("Exception during thread joining occurred.");
        }
    }

    @Test
    public void testUUIDCounter() {

        TransactionRequest request = TransactionRequest.newBuilder()
                .addQueries("CREATE (n:Person {name:'Sjoerd'})")
                .addQueries("CREATE (m:Person {name:'Silver'})")
                .addQueries("MATCH (n:Person),(m:Person) " +
                        "WHERE n.name = 'Sjoerd' AND m.name = 'Silver' " +
                        "CREATE (n)-[t:TO]->(m)")
                .setUUIDPrefix(TEST_PREFIX)
                .build();
        TransactionResponse response = blockingStub.execute(request);

        assert (response.getResult() == Status.SUCCESS);
        assert  (response.getModifications().getCreatedNodes(0).getNodeUUID().equals("myPrefix_0"));
        assert  (response.getModifications().getCreatedNodes(1).getNodeUUID().equals("myPrefix_1"));
        assert  (response.getModifications().getCreatedRelationships(0).getRelationshipUUID().equals("myPrefix_2"));


        request = TransactionRequest.newBuilder().addQueries("CREATE (o)").setUUIDPrefix("myPrefix1").build();
        response = blockingStub.execute(request);

        assert  (response.getModifications().getCreatedNodes(0).getNodeUUID().equals("myPrefix1_0"));
        assert (response.getResult() == Status.SUCCESS);

        request = TransactionRequest.newBuilder()
                .addQueries("CREATE (p:Person {name:'John'})")
                .addQueries("MATCH (p:Person) WHERE p.name='John' DELETE p")
                .addQueries("CREATE (q)")
                .setUUIDPrefix(TEST_PREFIX + "2")
                .build();
        response = blockingStub.execute(request);

        assert  (response.getModifications().getCreatedNodes(0).getNodeUUID().equals("myPrefix2_0"));
        assert  (response.getModifications().getCreatedNodesCount()==1);
        assert (response.getResult() == Status.SUCCESS);
    }

    @Test
    public void testAllTransactionDataResponseFields() {
        TransactionRequest request = TransactionRequest.newBuilder()
                .addQueries("CREATE (n:Person {name:'Sjoerd'})")
                .addQueries("CREATE (m:Person {name:'Silver'})")
                .addQueries("MATCH (n:Person),(m:Person) " +
                        "WHERE n.name = 'Sjoerd' AND m.name = 'Silver' " +
                        "CREATE (n)-[t:TO{weight:1}]->(m)")
                .setUUIDPrefix(TEST_PREFIX)
                .build();
        TransactionResponse response = blockingStub.execute(request);

        assert (response.getResult() == Status.SUCCESS);
        assert  (response.getModifications().getCreatedNodesCount()==2);
        assert  (response.getModifications().getCreatedRelationshipsCount()==1);
        assert  (response.getModifications().getAssignedLabelsCount()==2);
        assert  (response.getModifications().getAssignedNodePropertiesCount()==2);
        assert  (response.getModifications().getAssignedRelationshipPropertiesCount()==1);

        request = TransactionRequest.newBuilder()
                .addQueries("MATCH (:Person)-[t:TO]->(:Person) " +
                        "DELETE t")
                .addQueries("MATCH (n:Person) " +
                        "WHERE n.name='Sjoerd' " +
                        "DELETE n")
                .addQueries("MATCH (m:Person) " +
                        "WHERE m.name='Silver' " +
                        "DELETE m")
                .setUUIDPrefix(TEST_PREFIX + "2")
                .build();
        response = blockingStub.execute(request);

        assert (response.getResult() == Status.SUCCESS);
        assert  (response.getModifications().getDeletedNodesCount()==2);
        assert  (response.getModifications().getDeletedRelationshipsCount()==1);
        assert  (response.getModifications().getRemovedLabelsCount()==2);
        assert  (response.getModifications().getRemovedNodePropertiesCount()==4); //2+2 with UUIDs
        assert  (response.getModifications().getRemovedRelationPropertiesCount()==2); //1+1 with UUIDs
    }
}
