package com.tramchester.graph;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.presentation.LatLong;
import org.apache.commons.io.FileUtils;
import org.neo4j.gis.spatial.SimplePointLayer;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.gis.spatial.encoders.SimplePointEncoder;
import org.neo4j.gis.spatial.pipes.GeoPipeFlow;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.logging.slf4j.Slf4jLogProvider;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GraphDatabase implements Startable {
    private static final Logger logger = LoggerFactory.getLogger(GraphDatabase.class);

    private final TramchesterConfig configuration;
    private GraphDatabaseService theDB;
    private SpatialDatabaseService spatialDatabaseService;
    private SimplePointLayer spatialLayer;

    public GraphDatabase(TramchesterConfig configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() {
        logger.info("start");

        String graphName = configuration.getGraphName();
        logger.info("Create or load graph " + graphName);
        File graphFile = new File(graphName);

        boolean rebuildGraph = configuration.getRebuildGraph();

        if (rebuildGraph) {
            logger.info("Deleting previous graph db for " + graphFile.getAbsolutePath());
            try {
                FileUtils.deleteDirectory(graphFile);
            } catch (IOException ioException) {
                logger.error("Error deleting the graph!", ioException);
            }
        }

        theDB = createGraphDatabaseService(graphFile);
        spatialDatabaseService = new SpatialDatabaseService(theDB);
        spatialLayer = (SimplePointLayer) spatialDatabaseService.getOrCreateLayer("stations",
                SimplePointEncoder.class, SimplePointLayer.class);
        logger.info("graph db ready for " + graphFile.getAbsolutePath());
    }

    private static GraphDatabaseService createGraphDatabaseService(File graphFile) {
        GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory().setUserLogProvider(new Slf4jLogProvider());

        GraphDatabaseBuilder builder = graphDatabaseFactory.
                newEmbeddedDatabaseBuilder(graphFile).
                loadPropertiesFromFile("config/neo4j.conf");

        GraphDatabaseService graphDatabaseService = builder.newGraphDatabase();
        if (!graphDatabaseService.isAvailable(1000)) {
            logger.error("DB Service is not available");
        }
        return graphDatabaseService;
    }

    @Override
    public void stop() {
        try {
            if (theDB==null) {
                logger.error("Unable to obtain GraphDatabaseService for shutdown");
            } else {
                if (theDB.isAvailable(1000)) {
                    logger.info("Shutting down graphDB");
                    theDB.shutdown();
                } else {
                    logger.warn("Graph reported unavailable, attempt shutdown anyway");
                    theDB.shutdown();
                }
            }
        } catch (Exception exceptionInClose) {
            logger.error("Exception during close down", exceptionInClose);
        }
    }


    public Transaction beginTx() {
        return theDB.beginTx();
    }

    public void createIndexs() {
        logger.info("Create DB indexes");
        try ( Transaction tx = theDB.beginTx() )
        {
            Schema schema = theDB.schema();
            schema.indexFor(TransportGraphBuilder.Labels.TRAM_STATION).on(GraphStaticKeys.ID).create();
            schema.indexFor(TransportGraphBuilder.Labels.BUS_STATION).on(GraphStaticKeys.ID).create();
            schema.indexFor(TransportGraphBuilder.Labels.ROUTE_STATION).on(GraphStaticKeys.ID).create();
            schema.indexFor(TransportGraphBuilder.Labels.PLATFORM).on(GraphStaticKeys.ID).create();
            schema.indexFor(TransportGraphBuilder.Labels.SERVICE).on(GraphStaticKeys.ID).create();
            schema.indexFor(TransportGraphBuilder.Labels.HOUR).on(GraphStaticKeys.ID).create();
            schema.indexFor(TransportGraphBuilder.Labels.MINUTE).on(GraphStaticKeys.ID).create();

            tx.success();
        }
    }

    public void waitForIndexesReady() {
        theDB.schema().awaitIndexesOnline(5, TimeUnit.SECONDS);

        theDB.schema().getIndexes().forEach(indexDefinition -> {
            logger.info(String.format("Index label %s keys %s",
                    indexDefinition.getLabels(), indexDefinition.getPropertyKeys()));
        });
    }

    public Node createNode(TransportGraphBuilder.Labels label) {
        return theDB.createNode(label);
    }

    public Node findNode(TransportGraphBuilder.Labels labels, String id, String timeNodeId) {
        return theDB.findNode(labels, id, timeNodeId);
    }

    public boolean isAvailable(int timeoutMilli) {
        return theDB.isAvailable(timeoutMilli);
    }

    public ResourceIterator<Node> findNodes(TransportGraphBuilder.Labels label) {
        return theDB.findNodes(label);
    }

    public List<GeoPipeFlow> findClosestPointsTo(LatLong latLong, double rangeInKM) {
        return spatialLayer.findClosestPointsTo(LatLong.getCoordinate(latLong), rangeInKM);
    }

    public void addToSpatialLayer(Node node) {
        spatialLayer.add(node);
    }

    public TraversalDescription traversalDescription() {
        return theDB.traversalDescription();
    }

    public boolean isAvailable(long timeoutMillis) {
        return theDB.isAvailable(timeoutMillis);
    }

    public Transaction beginTx(int timeout, TimeUnit timeUnit) {
        return theDB.beginTx(timeout, timeUnit);
    }

    public Node getNodeById(int nodeId) {
        return theDB.getNodeById(nodeId);
    }
}
