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

        TransactionRequest request = TransactionRequest.newBuilder().setUUIDPrefix("myPrefix").addQueries("CREATE (n:Person { name: 'Sjoerd', title: 'Developer' })").build();
        TransactionResponse response = blockingStub.executeTransaction(request);

        assert (response.getResult() == Status.SUCCESS);
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
