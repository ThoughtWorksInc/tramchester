package com.tramchester.mappers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;

@Deprecated
public class DateTimeJsonDeserializer extends JsonDeserializer<DateTime> {
    private static String pattern = "HH:mm"; // TODO change to TramTime

    @Override
    public DateTime deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);
        DateTimeFormatter format = DateTimeFormat.forPattern(pattern);
        return DateTime.parse(node.asText(), format);
    }
}
