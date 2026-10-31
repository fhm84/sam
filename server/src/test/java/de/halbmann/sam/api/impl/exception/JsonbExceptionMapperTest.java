package de.halbmann.sam.api.impl.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.json.bind.JsonbException;
import org.junit.jupiter.api.Test;

class JsonbExceptionMapperTest {

    @Test
    void exceptionOutsideABodyReader_staysA500() {
        JsonbException exception = new JsonbException("cannot serialise");

        assertEquals(500, new JsonbExceptionMapper().toResponse(exception).getStatus());
    }

    @Test
    void exceptionFromABodyReader_isA400() {
        JsonbException exception = new JsonbException("wrong shape");
        exception.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("org.example.JsonbMessageBodyReader", "readFrom", "JsonbMessageBodyReader.java", 1)
        });

        assertEquals(400, new JsonbExceptionMapper().toResponse(exception).getStatus());
    }
}
