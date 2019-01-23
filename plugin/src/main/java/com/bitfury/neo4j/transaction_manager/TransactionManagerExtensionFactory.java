package com.bitfury.neo4j.transaction_manager;

import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.extension.ExtensionType;
import org.neo4j.kernel.extension.KernelExtensionFactory;
import org.neo4j.kernel.impl.spi.KernelContext;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.logging.internal.LogService;


public class TransactionManagerExtensionFactory extends KernelExtensionFactory<TransactionManagerExtensionFactory.Dependencies> {

    public static final String SERVICE_NAME = "EXONUM_TRANSACTION_MANAGER";

    public TransactionManagerExtensionFactory() {
        super(ExtensionType.DATABASE, SERVICE_NAME);
    }

    public interface Dependencies {
        GraphDatabaseAPI graphdatabaseAPI();
        LogService log();
        Config getConfig();
    }

    @Override
    public Lifecycle newInstance(KernelContext kernelContext, final Dependencies dependencies) {
        GraphDatabaseAPI db = dependencies.graphdatabaseAPI();
        LogService logService = dependencies.log();
        return new TransactionManagerLifecycle(logService, db, dependencies);
    }

    public static class TransactionManagerLifecycle extends LifecycleAdapter {

        private final LogService logService;
        private final GraphDatabaseAPI db;
        private final Dependencies dependencies;

        private TransactionManager manager;
        private TransactionManagerEventHandler eventHandler;

        public TransactionManagerLifecycle(LogService logService, GraphDatabaseAPI db, Dependencies dependencies) {
            this.logService = logService;
            this.db = db;
            this.dependencies = dependencies;
        }


        @Override
        @SuppressWarnings("unchecked")
        public void start() {

            // Create TransactionManager
            manager = new TransactionManager(
                    this.db,
                    dependencies.getConfig(),
                    logService.getUserLog(TransactionManager.class)
            );

            // Create EventHandler
            eventHandler = new TransactionManagerEventHandler(
                    manager,
                    logService.getUserLog(TransactionManagerEventHandler.class)
            );

            db.registerTransactionEventHandler(eventHandler);

            manager.start();
        }

        @Override
        public void shutdown() {
            manager.shutdown();
        }
    }

}
