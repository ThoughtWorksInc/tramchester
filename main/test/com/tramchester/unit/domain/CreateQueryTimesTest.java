package com.tramchester.unit.domain;


import com.tramchester.config.DataSourceConfig;
import com.tramchester.domain.time.CreateQueryTimes;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

class CreateQueryTimesTest {

    private final LocalConfig config = new LocalConfig();

    private final CreateQueryTimes createQueryTimes = new CreateQueryTimes(config);

    @Test
    void shouldGenerateCorrectTimesForTrams() {

        TramTime seedTime = TramTime.of(13, 20);
        List<TramTime> result = createQueryTimes.generate(seedTime, false);
        Assertions.assertEquals(3,result.size());

        Assertions.assertEquals(seedTime, result.get(0));
        Assertions.assertEquals(seedTime.plusMinutes(12), result.get(1));
        Assertions.assertEquals(seedTime.plusMinutes(24), result.get(2));
    }

    @Test
    void shouldGenerateCorrectTimesForWalkAtStart() {

        TramTime seedTime = TramTime.of(13, 20);
        List<TramTime> result = createQueryTimes.generate(seedTime, true);
        Assertions.assertEquals(1,result.size());

        Assertions.assertEquals(seedTime, result.get(0));
    }

    private static class LocalConfig extends TestConfig {

        @Override
        protected List<DataSourceConfig> getDataSourceFORTESTING() {
            return Collections.emptyList();
        }

    }
}
