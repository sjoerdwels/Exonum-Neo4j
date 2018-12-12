package com.bitfury.neo4j.transaction_manager;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.kernel.configuration.Config;


public class RegisterTransactionManagerExtensionFactory extends KernelExtensionFactory<RegisterTransactionManagerExtensionFactory.Dependencies> {

    public static final String SERVICE_NAME = "EXONUM_TRANSACTION_MANAGER";

    @Override
    public Lifecycle newInstance(KernelContext kernelContext, final Dependencies dependencies) {
        return new LifecycleAdapter() {

            private TransactionManager manager;

            // TODO: Default server settings
            @Override
            public void start() {

                // Create TransactionManager
                manager = new TransactionManager(
                        dependencies.getGraphDatabaseService(),
                        dependencies.getConfig(),
                        dependencies.log()
                );

                manager.start();
            }

            @Override
            public void shutdown() {
                manager.shutdown();
            }
        };
    }

    interface Dependencies {
        GraphDatabaseService getGraphDatabaseService();

        LogService log();

        Config getConfig();
    }

    public RegisterTransactionManagerExtensionFactory() {
        super(SERVICE_NAME);
    }
}
