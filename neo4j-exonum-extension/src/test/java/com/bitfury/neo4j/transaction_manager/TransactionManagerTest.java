package com.bitfury.neo4j.transaction_manager;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.*;
import org.neo4j.harness.junit.Neo4jRule;

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

        TransactionRequest request = TransactionRequest.newBuilder().build();
        TransactionResponse response;

        try {
            response = blockingStub.verifyTransaction(request);
            assert (false);
        } catch (Exception ex) {
        }
    }

    @Test
    public void testSingleSuccessTransaction() {

        TransactionRequest request = TransactionRequest.newBuilder().addQueries(" CREATE (n)").build();
        TransactionResponse response = blockingStub.executeTransaction(request);

        assert(response.getResult() ==  Status.SUCCESS);
    }

    @Test
    public void testSingleFailedTransaction() {

        TransactionRequest request = TransactionRequest.newBuilder().addQueries("FakeQuery").build();
        TransactionResponse response = blockingStub.executeTransaction(request);

        assert(response.getResult() ==  Status.FAILURE);
    }

}
