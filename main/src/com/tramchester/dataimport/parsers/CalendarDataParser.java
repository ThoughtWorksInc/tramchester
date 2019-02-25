package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.CalendarData;

import java.time.LocalDate;

import static org.apache.commons.lang3.StringUtils.isNumeric;

public class CalendarDataParser implements CSVEntryParser<CalendarData> {
    public CalendarData parseEntry(String... data) {
        String serviceId = data[0];
        boolean monday = data[1].equals("1");
        boolean tuesday = data[2].equals("1");
        boolean wednesday = data[3].equals("1");
        boolean thursday = data[4].equals("1");
        boolean friday = data[5].equals("1");
        boolean saturday = data[6].equals("1");
        boolean sunday = data[7].equals("1");
        LocalDate start = LocalDate.now();
        if (isNumeric(data[8])) {
            int year = Integer.parseInt(data[8].substring(0, 4));
            int monthOfYear = Integer.parseInt(data[8].substring(4, 6));
            int dayOfMonth = Integer.parseInt(data[8].substring(6, 8));
            start = LocalDate.of(year, monthOfYear, dayOfMonth);
        }
        LocalDate end = LocalDate.now();
        if (isNumeric(data[9])) {
            int year = Integer.parseInt(data[9].substring(0, 4));
            int monthOfYear = Integer.parseInt(data[9].substring(4, 6));
            int dayOfMonth = Integer.parseInt(data[9].substring(6, 8));
            end = LocalDate.of(year, monthOfYear, dayOfMonth);
        }
        return new CalendarData(serviceId, monday, tuesday, wednesday, thursday, friday, saturday, sunday, start, end);
    }
}
