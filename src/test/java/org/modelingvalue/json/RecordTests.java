package org.modelingvalue.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordTests {
    @Test
    public void simple() {
        SimpleRecord r1 = new SimpleRecord("lalala", "1.0", 4711, true);

        String json = Json.toJson(r1);
        assertEquals("""
                     {"b":true,"i":4711,"name":"lalala","version":"1.0"}""", json);

        SimpleRecord r2 = Json.fromJson(SimpleRecord.class, json);

        assertEquals(r1.name(), r2.name());
        assertEquals(r1.version(), r2.version());
        assertEquals(r1.i(), r2.i());
        assertEquals(r1.b(), r2.b());
    }

    @Test
    public void complex() {
        ComplexRecord r1 = new ComplexRecord(3.1415F, new SimpleRecord("lalala", "1.0", 4711, true));

        String json = Json.toJson(r1);
        assertEquals("""
                     {"fff":3.1415,"r":{"b":true,"i":4711,"name":"lalala","version":"1.0"}}""", json);

        ComplexRecord r2 = Json.fromJson(ComplexRecord.class, json);

        assertEquals(r1.fff(), r2.fff());
        assertEquals(r1.r().version(), r2.r().version());
        assertEquals(r1.r().i(), r2.r().i());
        assertEquals(r1.r().b(), r2.r().b());
    }

    record SimpleRecord(String name, String version, int i, boolean b) {
    }

    record ComplexRecord(Float fff, SimpleRecord r) {
    }
}
