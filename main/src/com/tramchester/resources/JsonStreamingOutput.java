package com.tramchester.resources;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;

class JsonStreamingOutput<T> implements StreamingOutput {
    private static final Logger logger = LoggerFactory.getLogger(JsonStreamingOutput.class);

    private final Stream<T> theStream;
    private final Transaction txn;

    private final JsonFactory jsonFactory ;

    JsonStreamingOutput(Transaction txn, Stream<T> theStream) {
        this.txn = txn;
        this.theStream = theStream;
        ObjectMapper objectMapper = new ObjectMapper();
        jsonFactory = objectMapper.getFactory();
    }

    @Override
    public void write(final OutputStream outputStream)  {
        logger.info("Write stream to response");

        theStream.onClose(() -> {
            logger.info("Closed stream");
            txn.close();
        });

        try (final JsonGenerator jsonGenerator = jsonFactory.createGenerator(outputStream)) {
            jsonGenerator.writeStartArray();
            theStream.forEach(item -> {
                try {
                    logger.warn("Wrote item");
                    jsonGenerator.writeObject(item);
                    jsonGenerator.writeString(System.lineSeparator());
                    jsonGenerator.writeString(System.lineSeparator());
                    jsonGenerator.flush();
//                    outputStream.flush();
                } catch (IOException e) {
                    logger.error("Exception during streaming item "+item.toString() ,e);
                }
            });
            jsonGenerator.writeEndArray();
            jsonGenerator.flush();
        } catch (IOException e) {
           logger.warn("Exception during streaming", e);
        } finally {
            theStream.close();
            logger.info("Stream closed");
        }
    }
}