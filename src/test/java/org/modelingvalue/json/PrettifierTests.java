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

import org.junit.jupiter.api.RepeatedTest;

import java.util.Currency;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.modelingvalue.json.Json.toJson;
import static org.modelingvalue.json.JsonPrettyfier.pretty;
import static org.modelingvalue.json.JsonPrettyfier.prettyStrict;
import static org.modelingvalue.json.JsonPrettyfier.terse;

public class PrettifierTests {
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    @RepeatedTest(1)
    public void prettyJson() {
        assertEquals("[]", pretty(toJson(List.of())));
        assertEquals("[\n  []\n]", pretty(toJson(List.of(List.of()))));
        assertEquals("[\n  [],\n  []\n]", pretty(toJson(List.of(List.of(), List.of()))));

        assertEquals("{}", pretty(toJson(Map.of())));
        assertEquals("{\n  \"a\": []\n}", pretty(toJson(Map.of("a", List.of()))));
        assertEquals("{\n  \"a\": {},\n  \"b\": []\n}", pretty(toJson(Map.of("a", Map.of(), "b", List.of()))));

        assertEquals("{\n  \"EUR\": 1.3,\n  \"SVC\": 13.67\n}",
                pretty(toJson(Map.of(Currency.getInstance("EUR"), 1.3, Currency.getInstance("SVC"), 13.67))));
        assertEquals("{\"EUR\": 1.3}",
                pretty(toJson(Map.of(Currency.getInstance("EUR"), 1.3))));
        assertEquals("[\n  1,\n  2,\n  {\"three\": 3},\n  [\n    [\n      1,\n      2,\n      3\n    ]\n  ]\n]",
                pretty(toJson(TestObjects.getTestObject1())));
        assertEquals(
                "[^#1,^#2,^#{\"three\":@3},^#[^##[^###1,^###2,^###3^##]^#]^]",
                JsonPrettyfier.prettify(toJson(TestObjects.getTestObject1()), "#", "^","@", false));
        assertEquals(
                pretty(toJson(TestObjects.getTestObject1())),
                pretty(pretty(pretty(pretty(toJson(TestObjects.getTestObject1())))))
        );
    }

    @RepeatedTest(1)
    public void prettyStrictJson() {
        assertEquals("[\n]", prettyStrict(toJson(List.of())));
        assertEquals("[\n  [\n  ]\n]", prettyStrict(toJson(List.of(List.of()))));
        assertEquals("[\n  [\n  ],\n  [\n  ]\n]", prettyStrict(toJson(List.of(List.of(), List.of()))));

        assertEquals("{\n}", prettyStrict(toJson(Map.of())));
        assertEquals("{\n  \"a\": [\n  ]\n}", prettyStrict(toJson(Map.of("a", List.of()))));
        assertEquals("{\n  \"a\": {\n  },\n  \"b\": [\n  ]\n}", prettyStrict(toJson(Map.of("a", Map.of(), "b", List.of()))));

        assertEquals("{\n  \"EUR\": 1.3\n}",
                prettyStrict(toJson(Map.of(Currency.getInstance("EUR"), 1.3))));
        assertEquals("{\n  \"EUR\": 1.3,\n  \"SVC\": 13.67\n}",
                prettyStrict(toJson(Map.of(Currency.getInstance("EUR"), 1.3, Currency.getInstance("SVC"), 13.67))));
        assertEquals("[\n  1,\n  2,\n  {\n    \"three\": 3\n  },\n  [\n    [\n      1,\n      2,\n      3\n    ]\n  ]\n]",
                prettyStrict(toJson(TestObjects.getTestObject1())));
        assertEquals(
                "[^#1,^#2,^#{^##\"three\":@3^#},^#[^##[^###1,^###2,^###3^##]^#]^]",
                JsonPrettyfier.prettify(toJson(TestObjects.getTestObject1()), "#", "^", "@",true));
        assertEquals(
                prettyStrict(toJson(TestObjects.getTestObject1())),
                prettyStrict(prettyStrict(prettyStrict(prettyStrict(toJson(TestObjects.getTestObject1())))))
        );
    }
    @RepeatedTest(1)
    public void terseJson() {
        assertEquals("[]", terse(toJson(List.of())));
        assertEquals("[[]]", terse(toJson(List.of(List.of()))));
        assertEquals("[[],[]]", terse(toJson(List.of(List.of(), List.of()))));

        assertEquals("{}", terse(toJson(Map.of())));
        assertEquals("{\"a\":[]}", terse(toJson(Map.of("a", List.of()))));
        assertEquals("{\"a\":{},\"b\":[]}", terse(toJson(Map.of("a", Map.of(), "b", List.of()))));

        assertEquals("{\"EUR\":1.3}",
                terse(toJson(Map.of(Currency.getInstance("EUR"), 1.3))));
        assertEquals("{\"EUR\":1.3,\"SVC\":13.67}",
                terse(toJson(Map.of(Currency.getInstance("EUR"), 1.3, Currency.getInstance("SVC"), 13.67))));
        assertEquals("[1,2,{\"three\":3},[[1,2,3]]]",
                terse(toJson(TestObjects.getTestObject1())));
        assertEquals(
                terse(toJson(TestObjects.getTestObject1())),
                terse(terse(terse(terse(toJson(TestObjects.getTestObject1())))))
        );
    }

    @RepeatedTest(10)
    public void prettify() {
        String json = JsonCustomTests.readData("test.json");

        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            json = pretty(json);
        }
        long delta = System.currentTimeMillis() - t0;

        double mbps = (100.0 * 1000.0 * json.length()) / (1024.0 * 1024.0 * delta);
        System.err.printf("prettify-test:                  %7.2f Mb/s json\n", mbps);
        assertTrue(10.0 < mbps, "<10 Mb/s (you either have a very slow machine or some change has impacted the performance)");
    }
}
