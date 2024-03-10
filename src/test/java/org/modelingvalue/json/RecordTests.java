//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2024 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
//                                                                                                                       ~
//  Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in       ~
//  compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0   ~
//  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on  ~
//  an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the   ~
//  specific language governing permissions and limitations under the License.                                           ~
//                                                                                                                       ~
//  Maintainers:                                                                                                         ~
//      Wim Bast, Tom Brus                                                                                               ~
//                                                                                                                       ~
//  Contributors:                                                                                                        ~
//      Ronald Krijgsheld ✝, Arjan Kok, Carel Bast                                                                       ~
// --------------------------------------------------------------------------------------------------------------------- ~
//  In Memory of Ronald Krijgsheld, 1972 - 2023                                                                          ~
//      Ronald was suddenly and unexpectedly taken from us. He was not only our long-term colleague and team member      ~
//      but also our friend. "He will live on in many of the lines of code you see below."                               ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RecordTests {
    public static final String SIMPLE_EXP          = """
                                                     {"b":true,"i":4711,"name":"lalala","version":"1.0"}
                                                     """.replace("\n", "");
    public static final String SIMPLE_WITH_EXTRA   = """
                                                     {"b":true,"i":4711,"name":"lalala","extra":1234,"version":"1.0"}
                                                     """.replace("\n", "");
    public static final String SIMPLE_WITH_MISSING = """
                                                     {"b":true,"i":4711,"name":"lalala"}
                                                     """.replace("\n", "");
    public static final String COMPLEX_EXP         = """
                                                     {"fff":3.1415,"r":{"b":true,"i":4711,"name":"lalala","version":"1.0"}}
                                                     """.replace("\n", "");

    @Test
    public void simple() {
        SimpleRecord r1 = new SimpleRecord("lalala", "1.0", 4711, true);

        String json = Json.toJson(r1);
        assertEquals(SIMPLE_EXP, json);

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
        assertEquals(COMPLEX_EXP, json);

        ComplexRecord r2 = Json.fromJson(ComplexRecord.class, json);

        assertEquals(r1.fff(), r2.fff());
        assertEquals(r1.r().version(), r2.r().version());
        assertEquals(r1.r().i(), r2.r().i());
        assertEquals(r1.r().b(), r2.r().b());
    }

    @Test
    public void throwOnExtra() {
        assertThrows(IllegalArgumentException.class, () -> Json.fromJson(SimpleRecord.class, SIMPLE_WITH_EXTRA));
    }

    @Test
    public void goOnExtra() {
        Config config = new Config();
        config.ignoreUnkownFieldsInRecords = true;
        SimpleRecord r1 = FromJsonGeneric.fromJson(SimpleRecord.class, SIMPLE_WITH_EXTRA, config);

        assertEquals("lalala", r1.name());
        assertEquals("1.0", r1.version());
        assertEquals(4711, r1.i());
        assertTrue(r1.b());
    }

    @Test
    public void nullOnMissing() {
        SimpleRecord r2 = Json.fromJson(SimpleRecord.class, SIMPLE_WITH_MISSING);

        assertEquals("lalala", r2.name());
        assertEquals(null, r2.version());
        assertEquals(4711, r2.i());
        assertTrue(r2.b());
    }

    record SimpleRecord(String name, String version, int i, boolean b) {
    }

    record ComplexRecord(Float fff, SimpleRecord r) {
    }
}
