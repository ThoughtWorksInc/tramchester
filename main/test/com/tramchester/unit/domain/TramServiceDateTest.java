package com.tramchester.unit.domain;

import com.tramchester.domain.time.TramServiceDate;
import org.junit.Test;

import java.time.LocalDate;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

public class TramServiceDateTest {

    @Test
    public void shouldInitiateWithDate() {
        LocalDate date = LocalDate.of(2014, 10, 25);
        TramServiceDate tramServiceDate = new TramServiceDate(date);

        assertThat(tramServiceDate.getDate()).isEqualTo(date);
        assertThat(tramServiceDate.getStringDate()).isEqualTo("20141025");
    }

    @Test
    public void shouldCheckBetween() {
        LocalDate aDate = LocalDate.of(2016, 6, 13);
        TramServiceDate tramServiceDate = new TramServiceDate(aDate);

        assertTrue(tramServiceDate.within(aDate, aDate.plusDays(1)));
        assertTrue(tramServiceDate.within(aDate.minusDays(1), aDate));
        assertTrue(tramServiceDate.within(aDate, aDate));
        assertTrue(tramServiceDate.within(aDate.minusDays(1), aDate.plusDays(1)));
        assertFalse(tramServiceDate.within(aDate.minusDays(2), aDate.minusDays(1)));
        assertFalse(tramServiceDate.within(aDate.plusDays(1), aDate.plusDays(2)));

    }
}