package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.repository.DataSourceRepository;
import org.apache.commons.io.FileUtils;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphalgo.BasicEvaluationContext;
import org.neo4j.graphalgo.EvaluationContext;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.logging.slf4j.Slf4jLogProvider;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class GraphDatabase implements Startable {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabase.class);

    private final TramchesterConfig configuration;
    private final DataSourceRepository transportData;
    private boolean cleanDB;
    private GraphDatabaseService databaseService;
    private DatabaseManagementService managementService;

    public GraphDatabase(TramchesterConfig configuration, DataSourceRepository transportData) {
        this.configuration = configuration;
        this.transportData = transportData;
    }

    @Override
    public void start() {
        logger.info("start");

        String graphName = configuration.getGraphName();
        logger.info("Create or load graph " + graphName);
        File graphFile = new File(graphName);
        boolean existingFile = graphFile.exists();

        cleanDB = !existingFile;
        if (existingFile) {
            logger.info("Graph db file is present at " + graphFile.getAbsolutePath());
        }

        databaseService = createGraphDatabaseService(graphFile);

        if (existingFile && !upToDate()) {
            cleanDB = true;
            logger.warn("Graph is out of data, rebuild needed");
            managementService.shutdown();
            int count = 10;
            while (databaseService.isAvailable(1000)) {
                logger.info("Waiting for graph shutdown");
                count--;
                if (count==0) {
                    throw new RuntimeException("Cannot shutdown out of date database");
                }
            }
            try {
                FileUtils.deleteDirectory(graphFile);
            } catch (IOException e) {
                String message = "Cannot delete out of date graph DB";
                logger.error(message,e);
                throw new RuntimeException(message,e);
            }
            graphFile = new File(graphName);
            databaseService = createGraphDatabaseService(graphFile);
        }

        logger.info("graph db started " + graphFile.getAbsolutePath());
    }

    public boolean isCleanDB() {
        return cleanDB;
    }

    private boolean upToDate() {
        DataSourceInfo info = transportData.getDataSourceInfo();
        logger.info("Checking graph version information ");

        Set<DataSourceInfo.NameAndVersion> versions = info.getVersions();

        // version -> flag
        Map<DataSourceInfo.NameAndVersion, Boolean> upToDate = new HashMap<>();
        try(Transaction transaction = beginTx()) {

            ResourceIterator<Node> query = findNodes(transaction, GraphBuilder.Labels.VERSION);
            List<Node> nodes = query.stream().collect(Collectors.toList());

            if (nodes.size()>1) {
                logger.error("Too many VERSION nodes, will use first");
            }

            if (nodes.isEmpty()) {
                logger.warn("Missing VERSION node, cannot check versions");
                return false;
            }

            Node versionNode = nodes.get(0);
            Map<String, Object> allProps = versionNode.getAllProperties();

            if (allProps.size()!=versions.size()) {
                logger.warn("VERSION node property mismatch, got " +allProps.size() + " expected " + versions.size());
                return false;
            }

            versions.forEach(nameAndVersion -> {
                String name = nameAndVersion.getName();
                logger.info("Checking version for " + name);

                if (allProps.containsKey(name)) {
                    String graphValue = allProps.get(name).toString();
                    boolean matches = nameAndVersion.getVersion().equals(graphValue);
                    upToDate.put(nameAndVersion, matches);
                    if (matches) {
                        logger.info("Got correct VERSION node value for " + nameAndVersion);
                    } else {
                        logger.warn(format("Mismatch on graph VERSION, got '%s' for %s", graphValue, nameAndVersion));
                    }
                } else {
                    upToDate.put(nameAndVersion, false);
                    logger.warn("Could not find version for " + name + " properties were " + allProps.toString());
                }
            });
        }
       return upToDate.values().stream().allMatch(flag -> flag);
    }

    private GraphDatabaseService createGraphDatabaseService(File graphFile) {

        managementService = new DatabaseManagementServiceBuilder( graphFile ).
                loadPropertiesFromFile("config/neo4j.conf").
                setUserLogProvider(new Slf4jLogProvider()).
                build();

        // for community edition must be DEFAULT_DATABASE_NAME
        GraphDatabaseService graphDatabaseService = managementService.database(DEFAULT_DATABASE_NAME);

        if (!graphDatabaseService.isAvailable(1000)) {
            logger.error("DB Service is not available, name: " + DEFAULT_DATABASE_NAME +
                    " Path: " + graphFile.toPath().toAbsolutePath());
        }
        return graphDatabaseService;
    }

    @Override
    public void stop() {
        try {
            if (databaseService ==null) {
                logger.error("Unable to obtain GraphDatabaseService for shutdown");
            } else {
                if (databaseService.isAvailable(1000)) {
                    logger.info("Shutting down graphDB");
                    managementService.shutdown();
                    logger.info("graphDB is shutdown");
                } else {
                    logger.warn("Graph reported unavailable, attempt shutdown anyway");
                    managementService.shutdown();
                }
            }
        } catch (Exception exceptionInClose) {
            logger.error("Exception during close down", exceptionInClose);
        }
    }

    public Transaction beginTx() {
        return databaseService.beginTx();
    }

    public Transaction beginTx(int timeout, TimeUnit timeUnit) {
        return databaseService.beginTx(timeout, timeUnit);
    }

    public void createIndexs() {
        logger.info("Create DB indexes");
        try ( Transaction tx = databaseService.beginTx() )
        {
            Schema schema = tx.schema();
            String idPropName = GraphPropertyKey.ID.getText();
            schema.indexFor(GraphBuilder.Labels.TRAM_STATION).on(idPropName).create();
            schema.indexFor(GraphBuilder.Labels.BUS_STATION).on(idPropName).create();
            schema.indexFor(GraphBuilder.Labels.ROUTE_STATION).on(idPropName).create();
            schema.indexFor(GraphBuilder.Labels.PLATFORM).on(idPropName).create();

            schema.indexFor(GraphBuilder.Labels.SERVICE).on(GraphPropertyKey.SERVICE_ID.getText()).create();
            schema.indexFor(GraphBuilder.Labels.HOUR).on(GraphPropertyKey.HOUR.getText()).create();
            schema.indexFor(GraphBuilder.Labels.MINUTE).on(GraphPropertyKey.TIME.getText()).create();

            // doesn't help graph build performance....
//            schema.indexFor(TransportRelationshipTypes.TO_SERVICE).on(GraphStaticKeys.TRIPS).
//                    withIndexType(IndexType.FULLTEXT).create();

            tx.commit();
        }
    }

    public void waitForIndexesReady(Transaction tx) {
        tx.schema().awaitIndexesOnline(5, TimeUnit.SECONDS);

        tx.schema().getIndexes().forEach(indexDefinition -> {
            logger.info(String.format("Index label %s keys %s",
                    indexDefinition.getLabels(), indexDefinition.getPropertyKeys()));
        });
    }

    public Node createNode(Transaction tx, GraphBuilder.Labels label) {
        return tx.createNode(label);
    }

    public Node findNode(Transaction tx, GraphBuilder.Labels labels, String idField, String idValue) {
        return tx.findNode(labels, idField, idValue);
    }

    public boolean isAvailable(int timeoutMilli) {
        return databaseService.isAvailable(timeoutMilli);
    }

    public ResourceIterator<Node> findNodes(Transaction tx, GraphBuilder.Labels label) {
        return tx.findNodes(label);
    }

    public TraversalDescription traversalDescription(Transaction tx) {
        return tx.traversalDescription();
    }

    public boolean isAvailable(long timeoutMillis) {
        return databaseService.isAvailable(timeoutMillis);
    }

    public EvaluationContext createContext(Transaction txn) {
        return new BasicEvaluationContext(txn, databaseService);
    }

}
