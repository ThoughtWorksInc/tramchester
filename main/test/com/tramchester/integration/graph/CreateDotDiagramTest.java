package com.tramchester.integration.graph;


import com.tramchester.Dependencies;
import com.tramchester.DiagramCreator;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.integration.Stations;
import org.junit.*;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.IOException;

public class CreateDotDiagramTest {
    private static Dependencies dependencies;
    private GraphDatabaseService graphService;
    private RelationshipFactory relationshipFactory;
    private NodeFactory nodeFactory;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @Before
    public void beforeEachOfTheTestsRun() {
        graphService = dependencies.get(GraphDatabaseService.class);
        relationshipFactory = dependencies.get(RelationshipFactory.class);
        nodeFactory = dependencies.get(NodeFactory.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldProduceADotDiagramOfTheTramNetwork() throws IOException {
        // TODO
        int depthLimit = 7;

        DiagramCreator creator = new DiagramCreator(nodeFactory, relationshipFactory, graphService, depthLimit);
        creator.create("manchester_trams.dot", Stations.Deansgate.getId());
    }

    // media city is somewhat unique....
    @Test
    public void shouldProduceADotDiagramOfTheTramNetworkForMediaCity() throws IOException {
        // TODO
        int depthLimit = 5;

        DiagramCreator creator = new DiagramCreator(nodeFactory, relationshipFactory, graphService, depthLimit);
        creator.create("mediacity_trams.dot", Stations.MediaCityUK.getId());
    }

}
