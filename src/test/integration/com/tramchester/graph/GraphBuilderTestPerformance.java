package com.tramchester.graph;

import com.tramchester.Dependencies;
import com.tramchester.TestConfig;
import org.joda.time.DateTime;
import org.junit.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GraphBuilderTestPerformance {

    private static Dependencies dependencies;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
    }

    @Before
    public void beforeEachTestRuns() {
        dependencies.get(RouteCalculator.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    @Ignore("For performance testing")
    public void shouldTestTimeToFileDataAndRebuildGraph() throws Exception {
        DateTime start = DateTime.now();

        dependencies.initialise(new PerformanceTestConfig());

        DateTime finished = DateTime.now();

        System.out.println("Initialisation took: " + finished.minus(start.getMillis()).getMillis());
    }

    private static class PerformanceTestConfig extends TestConfig {
        private Path outputPath = Paths.get("data/all");

        @Override
        public boolean isRebuildGraph() {
            return true;
        }

        @Override
        public boolean isFilterData() {
            return !outputPath.resolve("feed_info.txt").toFile().exists() ;
        }

        @Override
        public String getGraphName() {
            return "perf_test_tramchester.db";
        }

        @Override
        public Set<String> getAgencies() {
            return new HashSet(Arrays.asList("MET", "GMS"));
        }

        @Override
        public Path getInputDataPath() {
            return Paths.get("data/tram");
        }

        @Override
        public Path getOutputDataPath() {
            return outputPath;
        }

    }
}
