package com.tramchester.domain.presentation;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.CallsAtPlatforms;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Journey;
import com.tramchester.domain.Platform;
import com.tramchester.domain.liveUpdates.HasPlatformMessage;
import com.tramchester.domain.liveUpdates.PlatformMessage;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.PlatformMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.tramchester.domain.presentation.Note.NoteType.*;

@LazySingleton
public class ProvidesNotes {
    private static final Logger logger = LoggerFactory.getLogger(ProvidesNotes.class);

    private static final String EMPTY = "<no message>";
    public static final String website = "Please check <a href=\"http://www.metrolink.co.uk/pages/pni.aspx\">TFGM</a> for details.";
    public static final String weekend = "At the weekend your journey may be affected by improvement works." + website;
    public static final String christmas = "There are changes to Metrolink services during Christmas and New Year." + website;
    private static final int MESSAGE_LIFETIME = 5;

    private final PlatformMessageSource platformMessageSource;

    @Inject
    public ProvidesNotes(PlatformMessageSource platformMessageSource) {
        this.platformMessageSource = platformMessageSource;
    }

    public List<Note> createNotesForJourney(Journey journey, TramServiceDate queryDate) {
        List<Note> notes = new LinkedList<>();
        notes.addAll(createNotesForADate(queryDate));
        notes.addAll(liveNotesForJourney(journey, queryDate.getDate()));
        return notes;
    }

    public List<Note> createNotesForStations(List<Station> stations, TramServiceDate queryDate, TramTime time) {
        List<Note> notes = new LinkedList<>();
        notes.addAll(createNotesForADate(queryDate));
        notes.addAll(createLiveNotesForStations(stations, queryDate.getDate(), time));
        return notes;
    }

    private List<Note> createLiveNotesForStations(List<Station> stations, LocalDate date, TramTime time) {
        List<Note> notes = new ArrayList<>();

        stations.forEach(station -> platformMessageSource.messagesFor(station, date, time).forEach(info ->
                addRelevantNote(notes, info)));

        return notes;
    }

    private List<Note> createNotesForADate(TramServiceDate queryDate) {
        ArrayList<Note> notes = new ArrayList<>();
        if (queryDate.isWeekend()) {
            notes.add(new Note(weekend, Weekend));
        }
        if (queryDate.isChristmasPeriod()) {
            notes.add(new Note(christmas, Christmas));
        }
        return notes;
    }

    private <T extends CallsAtPlatforms> List<Note> liveNotesForJourney(T journey, LocalDate queryDate) {
        // Map: Note -> Location
        List<Note> notes = new ArrayList<>();

        // find all the platforms involved in a journey, so board, depart and changes
        // add messages for those platforms
        journey.getCallingPlatformIds().forEach(platform -> addLiveNotesForPlatform(notes, platform, queryDate,
                journey.getQueryTime()));

        return notes;
    }

    private void addLiveNotesForPlatform(List<Note> notes, IdFor<Platform> platformId, LocalDate queryDate, TramTime queryTime) {
        Optional<PlatformMessage> maybe = platformMessageSource.messagesFor(platformId, queryDate, queryTime);
        if (maybe.isEmpty()) {
            logger.warn("No messages found for " + platformId + " at " + queryDate +  " " + queryTime);
            return;
        }
        PlatformMessage info = maybe.get();
        LocalDateTime lastUpdate = info.getLastUpdate();
        if (!lastUpdate.toLocalDate().isEqual(queryDate)) {
            // message is not for journey time, perhaps journey is a future date or live data is stale
            logger.info("No messages available for " + queryDate + " last up date was " + lastUpdate);
            return;
        }
        TramTime updateTime = TramTime.of(lastUpdate.toLocalTime());
        // 1 minutes here as time sync on live api has been out by 1 min
        if (!queryTime.between(updateTime.minusMinutes(1), updateTime.plusMinutes(MESSAGE_LIFETIME))) {
            logger.info("No data available for " + queryTime + " as not between " + updateTime.minusMinutes(1) +
                    " and " + updateTime.plusMinutes(MESSAGE_LIFETIME));
            return;
        }
        addRelevantNote(notes, info);
    }

    private void addRelevantNote(List<Note> notes, HasPlatformMessage info) {

        String message = info.getMessage();
        if (usefulNote(message)) {
            logger.info("Added message from " + info);
            notes.add(new StationNote(Live, message, info.getStation()));
        } else {
            logger.info("Filtered message from " + info);
        }
    }

    private boolean usefulNote(String text) {
        return ! (text.isEmpty() || EMPTY.equals(text));
    }
}
