package com.tramchester.acceptance;

import com.tramchester.App;
import com.tramchester.acceptance.infra.AcceptanceAppExtenstion;
import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.acceptance.pages.App.AppPage;
import com.tramchester.acceptance.pages.App.Stage;
import com.tramchester.acceptance.pages.App.TestResultSummaryRow;
import com.tramchester.domain.reference.KnownTramRoute;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.resources.DataVersionResourceTest;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.Cookie;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.KnownTramRoute.AltrinchamPiccadilly;
import static com.tramchester.domain.reference.KnownTramRoute.VictoriaWythenshaweManchesterAirport;
import static com.tramchester.testSupport.reference.TramStations.Intu;
import static com.tramchester.testSupport.reference.TramStations.TraffordBar;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(DropwizardExtensionsSupport.class)
class AppUserJourneyTest extends UserJourneyTest {
    // NOTE: Needs correct locale settings, see .circleci/config.yml setupLocale target

    private static final String configPath = "config/localAcceptance.yml";

    private static final AcceptanceAppExtenstion appExtenstion = new AcceptanceAppExtenstion(App.class, configPath);

    private final String bury = TramStations.Bury.getName();
    private final String altrincham = TramStations.Altrincham.getName();
    private final String deansgate = TramStations.Deansgate.getName();

    // useful consts, keep around as can swap when timetable changes

    // No longer running
//    private static final String altyToBuryClass = "RouteClassGreenLine";
//    private static final String altyToBuryLineName = "Altrincham - Manchester - Bury";

    public static final String altyToPiccClass = getClassFor(AltrinchamPiccadilly);
    public static final String altyToPicLineName = "Altrincham - Piccadilly";

    private LocalDate when;
    private String url;

    @BeforeAll
    static void beforeAnyTestsRun() {
        createFactory(false);
    }

    private static Stream<ProvidesDriver> getProvider() {
        return getProviderCommon();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        url = appExtenstion.getUrl()+"/app/index.html";
        when = TestEnv.testDay();
    }

    @AfterEach
    void afterEachTestRuns(TestInfo testInfo) {
        takeScreenshotsFor(testInfo);
    }

    @AfterAll
    static void afterAllTestsRun() {
        closeFactory();
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldShowInitialCookieConsentAndThenDismiss(ProvidesDriver providesDriver) {
        providesDriver.init();
        providesDriver.clearCookies();

        AppPage appPage = providesDriver.getAppPage();
        appPage.load(url);

        assertTrue(appPage.waitForCookieAgreementVisible());
        appPage.agreeToCookies();
        assertTrue(appPage.waitForCookieAgreementInvisible());
        assertTrue(appPage.waitForReady());
        assertTrue(appPage.waitForToStops());
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldHaveInitialValuesAndSetInputsSetCorrectly(ProvidesDriver providesDriver) {
        AppPage appPage = prepare(providesDriver, url);

        validateCurrentTimeIsSelected(appPage);

        assertEquals(TestEnv.LocalNow().toLocalDate(), appPage.getDate());

        List<String> allFromStops = appPage.getAllStopsFromStops();
        checkSorted(allFromStops);

        List<String> allToStops = appPage.getAllStopsToStops();
        checkSorted(allToStops);

        desiredJourney(appPage, altrincham, bury, when, TramTime.of(10,15), false);
        assertJourney(appPage, altrincham, bury, "10:15", when, false);
        desiredJourney(appPage, altrincham, bury, when.plusMonths(1), TramTime.of(3,15), false);
        assertJourney(appPage, altrincham, bury, "03:15", when.plusMonths(1), false);

        appPage.selectNow();
        validateCurrentTimeIsSelected(appPage);

        appPage.selectToday();
        assertEquals(TestEnv.LocalNow().toLocalDate(), appPage.getDate());
    }

    private void checkSorted(List<String> allStops) {
        List<String> sortedAllStops = new LinkedList<>(allStops);
        sortedAllStops.sort(Comparator.comparing(String::toLowerCase));
        for (int i = 0; i < allStops.size(); i++) {
            assertEquals(sortedAllStops.get(i), allStops.get(i), "sorted?");
        }
    }

    private void validateCurrentTimeIsSelected(AppPage appPage) {
        LocalTime timeOnPage = timeSelectedOnPage(appPage);
        LocalTime now = TestEnv.LocalNow().toLocalTime();
        int diff = Math.abs(now.toSecondOfDay() - timeOnPage.toSecondOfDay());
        // allow for page render and webdriver overheads
        assertTrue(diff <= 110, String.format("now:%s timeOnPage: %s diff: %s", now, timeOnPage, diff));
    }

    private LocalTime timeSelectedOnPage(AppPage appPage) {
        String timeOnPageRaw = appPage.getTime();
        return LocalTime.parse(timeOnPageRaw);
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldTravelAltyToBuryAndSetRecents(ProvidesDriver providesDriver) {
        AppPage appPage = prepare(providesDriver, url);
        desiredJourney(appPage, altrincham, bury, when, TramTime.of(10,15), false);
        appPage.planAJourney();

        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        // so above station in recents
        appPage.setStart(TramStations.ExchangeSquare.getName()); // so alty is available in the recents list
        appPage.setDest(TramStations.PiccadillyGardens.getName()); // so bury is available in the recents list

        // check 'from' recents are set
        List<String> fromRecent = appPage.getRecentFromStops();
        assertThat(fromRecent, hasItems(altrincham, bury));

        List<String> remainingFromStops = appPage.getAllStopsFromStops();
        assertThat(remainingFromStops, not(contains(fromRecent)));
        // still displaying all stations
        assertEquals(TramStations.NumberOf-1, remainingFromStops.size()+fromRecent.size()); // less one as 'to' stop is excluded

        // check 'to' recents are set
        List<String> toRecent = appPage.getRecentToStops();
        assertThat(toRecent, hasItems(altrincham,bury));
        List<String> remainingToStops = appPage.getAllStopsToStops();
        assertThat(remainingToStops, not(contains(toRecent)));
        assertEquals(TramStations.NumberOf-1, remainingToStops.size()+toRecent.size()); // less one as 'from' stop is excluded

        // inputs still set
        assertJourney(appPage, TramStations.ExchangeSquare.getName(), TramStations.PiccadillyGardens.getName(), "10:15", when, false);
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldCheckAltrinchamToDeansgate(ProvidesDriver providesDriver) {
        AppPage appPage = prepare(providesDriver, url);
        TramTime queryTime = TramTime.of(10,0);
        desiredJourney(appPage, altrincham, deansgate, when, queryTime, false);
        appPage.planAJourney();

        assertTrue(appPage.resultsClickable());

        List<TestResultSummaryRow> results = appPage.getResults();
        // TODO Lockdown 3->2
        assertTrue(results.size()>=2, "at least 2 results");

        TramTime previous = queryTime;
        for (TestResultSummaryRow result : results) {
            TramTime currentArrivalTime = result.getArriveTime();
            assertTrue(currentArrivalTime.isAfter(previous) || currentArrivalTime.equals(previous),
                    "arrival time order for " + result + " previous: " + previous);
            assertTrue(currentArrivalTime.isAfter(result.getDepartTime()), "arrived before depart");
            assertEquals("Direct", result.getChanges());
            previous = currentArrivalTime;
        }

        // select first journey
        TestResultSummaryRow firstResult = results.get(0);
        firstResult.moveTo(providesDriver);
        appPage.waitForClickable(firstResult.getElement());
        firstResult.click(providesDriver);

        List<Stage> stages = firstResult.getStages();
        assertEquals(1, stages.size());
        Stage stage = stages.get(0);

        validateAStage(stage, firstResult.getDepartTime(), "Board Tram", altrincham, 1,
                altyToPiccClass, altyToPicLineName, TramStations.Piccadilly.getName(), 9);
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldCheckLateNightJourney(ProvidesDriver providesDriver) {
        AppPage appPage = prepare(providesDriver, url);
        TramTime queryTime = TramTime.of(23,30);
        desiredJourney(appPage, Intu.getName(), TraffordBar.getName(), when, queryTime, false);
        appPage.planAJourney();

        assertTrue(appPage.resultsClickable());

        List<TestResultSummaryRow> results = appPage.getResults();

        for (TestResultSummaryRow result : results) {
            TramTime arrivalTime = result.getArriveTime();
            assertTrue(arrivalTime.isAfter(TramTime.of(0,0)));
            assertTrue(arrivalTime.isBefore(TramTime.nextDay(1,0)));
        }
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldHideStationInToListWhenSelectedInFromList(ProvidesDriver providesDriver) {
        AppPage appPage = prepare(providesDriver, url);
        desiredJourney(appPage, altrincham, bury, when, TramTime.of(10,15), false);

        appPage.waitForStops(AppPage.FROM_STOP);
        List<String> destStops = appPage.getToStops();
        assertFalse(destStops.contains(altrincham), "should not contain alty");
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldShowNoRoutesMessage(ProvidesDriver providesDriver) {
        AppPage appPage = prepare(providesDriver, url);
        desiredJourney(appPage, altrincham, bury, when, TramTime.of(3,15), false);
        appPage.planAJourney();

        assertTrue(appPage.noResults());
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldUpdateWhenNewJourneyIsEntered(ProvidesDriver providesDriver) {
        TramTime tenFifteen = TramTime.of(10,15);
        TramTime eightFifteen = TramTime.of(9,15);

        AppPage appPage = prepare(providesDriver, url);
        desiredJourney(appPage, altrincham, bury, when, tenFifteen, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<TestResultSummaryRow> results = appPage.getResults();
        assertTrue(results.get(0).getDepartTime().isAfter(tenFifteen), "depart not after " + tenFifteen);

        desiredJourney(appPage, altrincham, bury, when, eightFifteen, false);
        appPage.planAJourney();
        // need way to delay response for this test to be useful
        //assertFalse(appPage.searchEnabled());
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<TestResultSummaryRow> updatedResults = appPage.getResults();
        assertTrue(updatedResults.get(0).getDepartTime().isBefore(tenFifteen));
        assertTrue(updatedResults.get(0).getDepartTime().isAfter(eightFifteen));
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldUpdateWhenEarlierClicked(ProvidesDriver providesDriver) {
        TramTime tenFifteen = TramTime.of(10,15);

        AppPage appPage = prepare(providesDriver, url);
        desiredJourney(appPage, altrincham, bury, when, tenFifteen, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<TestResultSummaryRow> results = appPage.getResults();
        TramTime firstDepartureTime = results.get(0).getDepartTime();
        assertTrue(firstDepartureTime.isAfter(tenFifteen));

        appPage.earlier();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<TestResultSummaryRow> updatedResults = appPage.getResults();
        TramTime updatedDepartTime = updatedResults.get(0).getDepartTime();
        assertTrue(updatedDepartTime.isBefore(firstDepartureTime), "should be before current first departure time");
        assertTrue(TramTime.diffenceAsMinutes(firstDepartureTime, updatedDepartTime)<60,
                "Too much gap between " + firstDepartureTime + " and update: " + updatedDepartTime);
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldUpdateWhenLaterClicked(ProvidesDriver providesDriver) {
        TramTime tenFifteen = TramTime.of(10,15);

        AppPage appPage = prepare(providesDriver, url);
        desiredJourney(appPage, altrincham, bury, when, tenFifteen, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<TestResultSummaryRow> results = appPage.getResults();
        assertTrue(results.get(0).getDepartTime().isAfter(tenFifteen));
        TramTime lastDepartureTime = results.get(results.size() - 1).getDepartTime();

        appPage.later();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.searchEnabled());

        List<TestResultSummaryRow> updatedResults = appPage.getResults();
        TramTime updatedDepartTime = updatedResults.get(0).getDepartTime();
        assertTrue(updatedDepartTime.isAfter(lastDepartureTime), "should be after current departure time");
        assertTrue(TramTime.diffenceAsMinutes(lastDepartureTime, updatedDepartTime)<60,
                "Too much gap between " + lastDepartureTime + " and update: " + updatedDepartTime);
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldHaveMultistageJourney(ProvidesDriver providesDriver) {
        AppPage appPage = prepare(providesDriver, url);
        TramTime planTime = TramTime.of(10,0);
        desiredJourney(appPage, altrincham, TramStations.ManAirport.getName(), when, planTime, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());

        List<TestResultSummaryRow> results = appPage.getResults();
        // TODO pre-lockdown timetable was 3
        assertTrue(results.size()>=2, "at least 2 journeys, was "+results.size());
        TramTime previousArrivalTime = planTime; // sorted by arrival time, so we may seen
        for (TestResultSummaryRow result : results) {
            TramTime arriveTime = result.getArriveTime();
            assertTrue(arriveTime.isAfter(result.getDepartTime()));
            assertTrue(arriveTime.isAfter(previousArrivalTime) || arriveTime.equals(previousArrivalTime));
            assertEquals(result.getChanges(), TraffordBar.getName());
            previousArrivalTime = arriveTime;
        }

        // select first journey
        TestResultSummaryRow firstResult = results.get(0);
        firstResult.moveTo(providesDriver);
        appPage.waitForClickable(firstResult.getElement());
        firstResult.click(providesDriver);

        List<Stage> stages = firstResult.getStages();
        assertEquals(2, stages.size());

        Stage firstStage = stages.get(0);
        Stage secondStage = stages.get(1);

        validateAStage(firstStage, firstResult.getDepartTime(), "Board Tram", altrincham, 1,
                altyToPiccClass, altyToPicLineName,
                TramStations.Piccadilly.getName(), 7);

        // Too timetable dependent?
        String lineClass = getClassFor(VictoriaWythenshaweManchesterAirport);
        validateAStage(secondStage, TramTime.of(10,32), "Change Tram", TraffordBar.getName(),
                2, lineClass, "Victoria - Wythenshawe - Manchester Airport",
                TramStations.ManAirport.getName(), 17);

        assertEquals(TraffordBar.getName(), secondStage.getActionStation());
        assertEquals("Change Tram", secondStage.getAction());

    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldDisplayWeekendWorkNoteOnlyOnWeekends(ProvidesDriver providesDriver) {
        TramTime time = TramTime.of(10,15);

        AppPage appPage = prepare(providesDriver, url);
        LocalDate aSaturday = TestEnv.nextSaturday();

        desiredJourney(appPage, altrincham, bury, aSaturday, time, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.notesPresent());
        assertTrue(appPage.hasWeekendMessage());

        desiredJourney(appPage, altrincham, bury, when, time, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.noWeekendMessage());

        desiredJourney(appPage, altrincham, bury, aSaturday.plusDays(1), time, false);
        appPage.planAJourney();
        assertTrue(appPage.resultsClickable());
        assertTrue(appPage.notesPresent());
        assertTrue(appPage.hasWeekendMessage());
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldHaveBuildAndVersionNumberInFooter(ProvidesDriver providesDriver) {
        AppPage appPage = prepare(providesDriver, url);

        String build = appPage.getExpectedBuildNumberFromEnv();

        String result = appPage.getBuild();
        assertEquals("2."+build, result);

        String version = appPage.getVersion();
        assertEquals(DataVersionResourceTest.version, version);


    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldDisplayCookieAgreementIfNotVisited(ProvidesDriver providesDriver) {
        providesDriver.init();
        providesDriver.clearCookies();

        AppPage appPage = providesDriver.getAppPage();
        appPage.load(url);

        assertNull(providesDriver.getCookieNamed("tramchesterVisited"));

        assertTrue(appPage.waitForCookieAgreementVisible());

        appPage.agreeToCookies();
        assertTrue(appPage.waitForCookieAgreementInvisible(), "wait for cookie agreement to close");

        // cookie should now be set
        Cookie cookie = providesDriver.getCookieNamed("tramchesterVisited");
        assertNotNull(cookie, "cookie null");
        assertNotNull(cookie.getValue(), "cookie null");

        String cookieContents = URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
        assertEquals("{\"visited\":true}", cookieContents);
        assertTrue(appPage.waitForCookieAgreementInvisible());

        AppPage afterReload = providesDriver.getAppPage();
        assertTrue(afterReload.waitForCookieAgreementInvisible());
        afterReload.waitForToStops();
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getProvider")
    void shouldDisplayDisclaimer(ProvidesDriver providesDriver) {
        AppPage appPage = prepare(providesDriver, url);

        appPage.displayDisclaimer();
        assertTrue(appPage.waitForDisclaimerVisible());

        appPage.dismissDisclaimer();
        // chrome takes a while to close it, so wait for it to go
        assertTrue(appPage.waitForDisclaimerInvisible());
    }

    public static void desiredJourney(AppPage appPage, String start, String dest, LocalDate date, TramTime time, boolean arriveBy) {
        appPage.setStart(start);
        appPage.setDest(dest);
        appPage.setSpecificDate(date);
        appPage.setTime(time);
        appPage.setArriveBy(arriveBy);
    }

    private static void assertJourney(AppPage appPage, String start, String dest, String time, LocalDate date, boolean arriveBy) {
        assertEquals(start, appPage.getFromStop());
        assertEquals(dest, appPage.getToStop());
        assertEquals(time, appPage.getTime());
        assertEquals(date, appPage.getDate());
        assertEquals(arriveBy, appPage.getArriveBy());
    }

    public static void validateAStage(Stage stage, TramTime departTime, String action, String actionStation, int platform,
                                      String lineClass, String lineName, String headsign, int passedStops) {
        assertEquals(departTime, stage.getDepartTime(), "departTime");
        assertEquals(action, stage.getAction(), "action");
        assertEquals(actionStation, stage.getActionStation(), "actionStation");
        assertEquals(platform, stage.getPlatform(), "platform");
        assertEquals(lineName, stage.getLine(lineClass), "lineName");
        assertEquals(headsign, stage.getHeadsign(), "headsign");
        assertEquals(passedStops, stage.getPassedStops(), "passedStops");
    }

    @NotNull
    private static String getClassFor(KnownTramRoute knownTramRoute) {
        return "RouteClass"+ knownTramRoute.shortName().replaceAll(" ","");
    }

}

