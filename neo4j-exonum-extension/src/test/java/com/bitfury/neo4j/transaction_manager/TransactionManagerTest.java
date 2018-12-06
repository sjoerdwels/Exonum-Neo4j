package com.bitfury.neo4j.transaction_manager;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.*;
import org.neo4j.harness.junit.Neo4jRule;

import java.util.Random;

public class TransactionManagerTest {

    @Rule
    public final Neo4jRule neo4j = new Neo4jRule();

    private static TransactionManagerGrpc.TransactionManagerBlockingStub blockingStub;

    @Before
    public void setup() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9994)
                .usePlaintext(true)
                .build();
        blockingStub = TransactionManagerGrpc.newBlockingStub(channel);
    }

    @Test
    public void testEmptyTransaction() {

        TransactionRequest request = TransactionRequest.newBuilder().setUUIDPrefix("myPrefix").build();

        try {
            blockingStub.verifyTransaction(request);
            assert (false);
        } catch (Exception ex) {
        }
    }

    @Test
    public void testSingleSuccessTransaction() {

        System.out.println("Insert node.");

        TransactionRequest request = TransactionRequest.newBuilder().setUUIDPrefix("myPrefix").addQueries("CREATE (n:Person { name: 'Sjoerd', title: 'Developer' })").build();
        TransactionResponse response = blockingStub.executeTransaction(request);

        assert (response.getResult() == Status.SUCCESS);

        System.out.println(response.toString());
        System.out.println("Update property.");

        request = TransactionRequest.newBuilder().setUUIDPrefix("myPrefix2").addQueries("MATCH (n:Person) SET n.name =  'peter'").build();
        response = blockingStub.executeTransaction(request);

        assert (response.getResult() == Status.SUCCESS);

        System.out.println(response.toString());
        System.out.println("Delete node.");

        request = TransactionRequest.newBuilder().setUUIDPrefix("myPrefix3").addQueries("MATCH (n:Person) DELETE n").build();
        response = blockingStub.executeTransaction(request);

        System.out.println(response.toString());

        assert (response.getResult() == Status.SUCCESS);
    }

    @Test
    public void testModifyUUIDTransaction() {

        System.out.println("Insert node.");

        TransactionRequest request = TransactionRequest.newBuilder().setUUIDPrefix("myPrefix").addQueries("CREATE (n:Person { id: '2', title: 'Developer' })").build();
        TransactionResponse response = blockingStub.executeTransaction(request);

        assert (response.getResult() == Status.SUCCESS);

        System.out.println(response.toString());
        System.out.println("Update UUID property.");

        request = TransactionRequest.newBuilder().setUUIDPrefix("myPrefix2").addQueries("MATCH (n:Person) SET n."+ Properties.UUID + " =  'modifiedUUID'").build();
        response = blockingStub.executeTransaction(request);

        assert (response.getResult() == Status.FAILURE);

        System.out.println(response.toString());
    }

    @Test
    public void testSingleFailedTransaction() {

        TransactionRequest request = TransactionRequest.newBuilder().setUUIDPrefix("myPrefix").addQueries("FakeQuery").build();
        TransactionResponse response = blockingStub.executeTransaction(request);

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

                TransactionRequest request = TransactionRequest.newBuilder().setUUIDPrefix("myPrefix").addQueries(" CREATE (n)").build();
                TransactionResponse response = blockingStub.verifyTransaction(request);

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

}
