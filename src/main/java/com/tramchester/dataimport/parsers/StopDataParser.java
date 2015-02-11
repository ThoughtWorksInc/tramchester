package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.StopData;

public class StopDataParser implements CSVEntryParser<StopData> {
    public StopData parseEntry(String... data) {
        String id = data[0].substring(0, data[0].length() - 1);
        String code = data[1];
        String name = data[2].split(",")[1].replace(" (Manchester Metrolink)", "").replace("\"", "").trim();
        String latitude = data[4];
        String longitude = data[5];

        return new StopData(id, code, name, latitude, longitude);
    }
}