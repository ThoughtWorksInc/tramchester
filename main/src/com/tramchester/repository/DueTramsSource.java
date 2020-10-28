package com.tramchester.repository;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.PlatformDueTrams;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DueTramsSource extends LiveDataCache {
    Optional<PlatformDueTrams> allTrams(IdFor<Platform> platform, LocalDate tramServiceDate, TramTime queryTime);
    List<DueTram> dueTramsFor(Station station, LocalDate date, TramTime queryTime);
}
