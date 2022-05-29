//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2022 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus, Ronald Krijgsheld                                                                           ~
// Contributors:                                                                                                       ~
//     Arjan Kok, Carel Bast                                                                                           ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.json;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.modelingvalue.json.Json.fromJson;
import static org.modelingvalue.json.Json.toJson;

public class JsonTests {
    @RepeatedTest(1)
    public void primitivesToJson() {
        assertEquals("{}", toJson(new Object()));
        assertEquals("null", toJson(null));
        assertEquals("0", toJson((byte) 0));
        assertEquals("42", toJson((byte) 42));
        assertEquals("12", toJson((short) 12));
        assertEquals("21", toJson(21));
        assertEquals("1234567890", toJson((long) 1234567890));
        assertEquals("12.0", toJson((float) 12));
        assertEquals("12.0", toJson((double) 12));
        assertEquals("-1.26E-39", toJson(-12.6e-40));
        assertEquals(quoted("@"), toJson('@'));
        assertEquals(quoted("\\u0000"), toJson('\000'));
        assertEquals(quoted("\\u0000"), toJson('\u0000'));
        assertEquals(quoted("·"), toJson('\u00b7'));
        assertEquals(quoted("\\\""), toJson('"'));
        assertEquals(quoted("'"), toJson('\''));
        assertEquals("true", toJson(true));
        assertEquals(quoted("blabla-·-\\u0000\\\"-\\t\\r\\n\\f\\b/\\\\-\\u2022"), toJson("blabla-\u00b7-\000\"-\t\r\n\f\b/\\-\u2022"));
    }

    @RepeatedTest(1)
    public void listsToJson() {
        assertEquals("[]", toJson(new byte[]{}));
        assertEquals("[]", toJson(new int[]{}));
        assertEquals("[]", toJson(new String[]{}));
        assertEquals("[]", toJson(new ArrayList<>()));
        assertEquals("[true,false,true]", toJson(new boolean[]{true, false, true}));
        assertEquals("[1,2,3,4]", toJson(new byte[]{1, 2, 3, 4}));
        assertEquals("[\"a\",\"b\",\"c\",\"d\"]", toJson(new char[]{'a', 'b', 'c', 'd'}));
        assertEquals("[\"a\",\"b\",\"c\",\"d\"]", toJson(new String[]{"a", "b", "c", "d"}));
        assertEquals("[1,2,3,4]", toJson(new short[]{1, 2, 3, 4}));
        assertEquals("[1,2,3,4]", toJson(new int[]{1, 2, 3, 4}));
        assertEquals("[1,2,3,4]", toJson(new long[]{1, 2, 3, 4}));
        assertEquals("[1.0,2.0,3.0,4.0]", toJson(new float[]{1, 2, 3, 4}));
        assertEquals("[1.0,2.0,3.0,4.0]", toJson(new double[]{1, 2, 3, 4}));
        assertEquals("[2711.9,\"EUR\",21]", toJson(new Object[]{2711.9, "EUR", 21}));

        assertEquals("[1,2,3]", toJson(new ArrayList<>(Arrays.asList(1, 2, 3))));
        assertEquals("[1,2,3,[1,2,3,[1,2,3]]]", toJson(getTestObject1()));
        assertEquals("[1,2,3]", toJson(Arrays.asList(1, 2, 3)));
        assertEquals("[1,\"a\",3,12.6,\"q\"]", toJson(Arrays.asList(1, "a", 3L, 12.6, 'q')));
    }

    @RepeatedTest(1)
    public void mapsToJson() {
        assertEquals("{}",
                toJson(Map.of()));
        assertEquals("{\"a\":1,\"b\":2}",
                toJson(Map.of("a", 1, "b", 2)));
        assertEquals("{\"a\":\"a\"}",
                toJson(makeMap(new SimpleEntry<>("a", "a"))));
        assertEquals("{\"a\":null,\"b\":null,\"c\":null}",
                toJson(makeMap(new SimpleEntry<>("a", null), new SimpleEntry<>("b", null), new SimpleEntry<>("c", null))));
        assertThrows(NullPointerException.class, () -> toJson(makeMap(new SimpleEntry<>(null, "a"), new SimpleEntry<>("b", "b"), new SimpleEntry<>("c", "c"))));
        assertThrows(NullPointerException.class, () -> toJson(makeMap(new SimpleEntry<>(null, null), new SimpleEntry<>("null", null))));
        assertThrows(NullPointerException.class, () -> toJson(makeMap(new SimpleEntry<>(null, null))));
        assertThrows(NullPointerException.class, () -> toJson(makeMap(new SimpleEntry<>(null, "a"))));
        assertEquals("{\"1\":1,\"b\":2}",
                toJson(Map.of(1, 1, "b", 2)));
        assertEquals("{\"EUR\":1.3,\"SVC\":13.67}",
                toJson(Map.of(Currency.getInstance("EUR"), 1.3, Currency.getInstance("SVC"), 13.67)));
        assertEquals("{\"EUR\":21,\"SVC\":{}}",
                toJson(Map.of(Currency.getInstance("EUR"), 21, Currency.getInstance("SVC"), new Object())));
        assertEquals("{\"EUR\":{\"currencyCode\":\"EUR\",\"defaultFractionDigits\":2,\"displayName\":\"Euro\",\"numericCode\":978,\"numericCodeAsString\":\"978\",\"symbol\":\"\\u20AC\"},\"SVC\":{}}",
                toJson(Map.of(Currency.getInstance("EUR"), Currency.getInstance("EUR"), Currency.getInstance("SVC"), new Object())));
    }

    @RepeatedTest(1)
    public void prettyJson() {
        assertEquals("[\n]", JsonPrettyfier.pretty(toJson(List.of())));
        assertEquals("[\n  [\n  ]\n]", JsonPrettyfier.pretty(toJson(List.of(List.of()))));
        assertEquals("[\n  [\n  ],\n  [\n  ]\n]", JsonPrettyfier.pretty(toJson(List.of(List.of(), List.of()))));

        assertEquals("{\n}", JsonPrettyfier.pretty(toJson(Map.of())));
        assertEquals("{\n  \"a\": [\n  ]\n}", JsonPrettyfier.pretty(toJson(Map.of("a", List.of()))));
        assertEquals("{\n  \"a\": {\n  },\n  \"b\": [\n  ]\n}", JsonPrettyfier.pretty(toJson(Map.of("a", Map.of(), "b", List.of()))));

        assertEquals("{\n  \"EUR\": 1.3,\n  \"SVC\": 13.67\n}",
                JsonPrettyfier.pretty(toJson(Map.of(Currency.getInstance("EUR"), 1.3, Currency.getInstance("SVC"), 13.67))));
        assertEquals("[\n  1,\n  2,\n  3,\n  [\n    1,\n    2,\n    3,\n    [\n      1,\n      2,\n      3\n    ]\n  ]\n]",
                JsonPrettyfier.pretty(toJson(getTestObject1())));
        assertEquals(
                "[^#1,^#2,^#3,^#[^##1,^##2,^##3,^##[^###1,^###2,^###3^##]^#]^]",
                JsonPrettyfier.pretty(toJson(getTestObject1()), "#", "^"));
        assertEquals(
                JsonPrettyfier.pretty(toJson(getTestObject1())),
                JsonPrettyfier.pretty(JsonPrettyfier.pretty(JsonPrettyfier.pretty(JsonPrettyfier.pretty(toJson(getTestObject1())))))
        );
    }

    @Test
    public void primitivesFromJson() {
        assertThrows(NullPointerException.class, () -> fromJson(null));
        assertThrows(IllegalArgumentException.class, () -> fromJson(""));
        assertThrows(IllegalArgumentException.class, () -> fromJson("nuk"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("flase"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("falseify"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("treu"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("null x"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("null#"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("n"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("tof"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("     "));
        assertThrows(IllegalArgumentException.class, () -> fromJson("  \n\r   "));
        assertThrows(IllegalArgumentException.class, () -> fromJson("  ^   "));
        assertThrows(IllegalArgumentException.class, () -> fromJson("  ◊   "));
        assertThrows(IllegalArgumentException.class, () -> fromJson("  \\u"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("  \\u1"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("  \\u12"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("  \\u123"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("  \"\\u\""));
        assertThrows(IllegalArgumentException.class, () -> fromJson("  \"\\u1\""));
        assertThrows(IllegalArgumentException.class, () -> fromJson("  \"\\u12\""));
        assertThrows(IllegalArgumentException.class, () -> fromJson("  \"\\u123\""));
        assertThrows(IllegalArgumentException.class, () -> fromJson("  \"\\q\""));
        assertThrows(IllegalArgumentException.class, () -> fromJson("NULL"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("TRUE"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("FALSE"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("010"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("01e"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("-010"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("-"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("- "));
        assertThrows(IllegalArgumentException.class, () -> fromJson("-a"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("- a"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("-0e"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("- 0"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("+010"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("+"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("+ "));
        assertThrows(IllegalArgumentException.class, () -> fromJson("+a"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("+ a"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("+0"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("+0e"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("+ 0"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("."));
        assertThrows(IllegalArgumentException.class, () -> fromJson(". "));
        assertThrows(IllegalArgumentException.class, () -> fromJson(".a"));
        assertThrows(IllegalArgumentException.class, () -> fromJson(". a"));
        assertThrows(IllegalArgumentException.class, () -> fromJson(".0"));
        assertThrows(IllegalArgumentException.class, () -> fromJson(".0e"));
        assertThrows(IllegalArgumentException.class, () -> fromJson(". 0"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("e"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("e "));
        assertThrows(IllegalArgumentException.class, () -> fromJson("ea"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("e a"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("e0"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("e0e"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("e 0"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("12345678901234567890"));

        assertNull(fromJson("null"));
        assertEquals(true, fromJson("true"));
        assertEquals(false, fromJson("false"));

        assertEquals(0L, fromJson("0"));
        assertEquals(0L, fromJson("-0"));
        assertEquals(4L, fromJson("4"));
        assertEquals(42L, fromJson("42"));
        assertEquals(12L, fromJson("12"));
        assertEquals(21L, fromJson("21"));
        assertEquals(1L, fromJson("1"));
        assertEquals(13L, fromJson("13"));
        assertEquals(133L, fromJson("133"));
        assertEquals(Long.MAX_VALUE, fromJson(Long.toString(Long.MAX_VALUE)));
        assertEquals(Long.MIN_VALUE, fromJson(Long.toString(Long.MIN_VALUE)));
        assertEquals(1234567890123456789L, fromJson("1234567890123456789"));
        assertEquals(1L, fromJson("1"));
        assertEquals(1234567890L, fromJson("1234567890"));
        assertEquals(12.0, fromJson("12.0"));
        assertEquals(120000.0, fromJson("12e4"));
        assertEquals(-12.6e-40, fromJson("-1.26E-39"));
        assertEquals(-1.26E39, fromJson("-1.26E+39"));
        assertEquals(-0.0, fromJson("-0e-2"));
        assertEquals(0.0, fromJson("0e-2"));

        assertEquals("@", fromJson(quoted("@")));
        assertEquals("\000", fromJson(quoted("\\u0000")));
        assertEquals("\u0000", fromJson(quoted("\\u0000")));
        assertEquals("\ubaff", fromJson(quoted("\\ubaff")));
        assertEquals("\u00b7", fromJson(quoted("·")));
        assertEquals("\"", fromJson(quoted("\\\"")));
        assertEquals("'", fromJson(quoted("'")));
        assertEquals("blabla-\u00b7-\000\"-\t\r\n\f\b/\\-\u2022", fromJson(quoted("blabla-·-\\u0000\\\"-\\t\\r\\n\\f\\b\\/\\\\-\\u2022")));
    }

    @Test
    public void arraysFromJson() {
        assertThrows(IllegalArgumentException.class, () -> fromJson("[1,2 3,[1,2,3,[1,2,3]]]"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("[1,2 3,[1,2,3,[1,2,3]],]"));

        assertEquals(getTestObject1(), fromJson("[1,2,3,[1,2,3,[1,2,3]]]"));
        assertEquals(getTestObject1(), fromJson("   [\n  1,2,\n  3,[\r\t\n    1,\n    2,\n    3,\n    [\n      1,\n      2,\n      3\n    ]\n  ]\n]\n\n\n"));
    }

    @Test
    public void mapsFromJson() {
        assertThrows(IllegalArgumentException.class, () -> fromJson("{\"a\":1,\"b\":2,}"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("{\"a\":1,\"b\":2,\"x\"}"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("{\"a\":1,\"b\":2,\"x\";}"));
        assertThrows(IllegalArgumentException.class, () -> fromJson("{\"a\":1,\"b\":2;}"));
        assertEquals(Map.of("a", 1L, "b", 2L), fromJson("{\"a\":1,\"b\":2}"));
    }

    @Test
    public void objectFromJson() {
        String json_1 = "{" +
                        // TODO
                        // "\"arrayOfAbstract\":[" +
                        // "    {\"$type\":\"org.modelingvalue.json.JsonTests$XXX# burp\",\"field\":3333,\"shared_field\":4713}," +
                        // "    {\"$type\":\"org.modelingvalue.json.JsonTests$YYY# why?\",\"field\":4444,\"shared_field\":4714}" +
                        // "],"+
                        "\"b-o\":true," +
                        "\"b_y+t#e\":44," +
                        "\"bool\":true," +
                        "\"ch\":44," +
                        "\"db\":3.1415," +
                        "\"fl\":4711.2," +
                        "\"in\":123456789," +
                        "\"li1\":[1,2,3]," +
                        "\"li2\":[{\"id\":1},{\"id\":2,\"sub\":{\"id\":21}},{\"id\":3,\"sub\":{\"id\":31}}]," +
                        "\"li3\":[[{\"id\":1}],[{\"id\":2}]]," +
                        "\"listOfAbstract\":[" +
                        "    {\"$type\":\"org.modelingvalue.json.JsonTests$XXX# burp\",\"field\":1111,\"shared_field\":4711}," +
                        "    {\"$type\":\"org.modelingvalue.json.JsonTests$YYY# why?\",\"field\":2222,\"shared_field\":4712}" +
                        "]," +
                        "\"lo\":1234567890123456789," +
                        "\"ma\":{\"nope\":false,\"sure\":true,\"yes\":true}," +
                        "\"num\":300," +
                        "\"ob1\":{\"id\":33,\"sub\":{\"id\":88.0,\"sub\":{\"id\":99}}}," +
                        "\"ob2\":{\"id\":\"44\"}," +
                        "\"sh\":10000," +
                        "\"ss\":[1,3,1,3]," +
                        "\"st\":\"burp••\"," +
                        "\"string\":\"burpie\"" +
                        "}";

        AAA o = fromJson(AAA.class, json_1);

        assertNotNull(o);
        assertTrue(o.bo);
        assertNotNull(o.num);
        assertEquals(300, o.num.num);
        assertNotNull(o.bool);
        assertTrue(o.bool.bool);
        assertNotNull(o.string);
        assertEquals("burpie", o.string.string);
        assertEquals(44, o.by);
        assertEquals(10000, o.sh);
        assertEquals(123456789, o.in);
        assertEquals(1234567890123456789L, o.lo);
        assertEquals("" + 3.1415D, "" + o.db);
        assertEquals("" + 4711.2, "" + o.fl);
        assertEquals("burp••", o.st);
        assertEquals((char) 44, o.ch);
        assertEquals(33, o.ob1.id);
        assertEquals(88, o.ob1.sub.id);
        assertEquals(99, o.ob1.sub.sub.id);
        assertEquals(44, o.ob2.id);
        assertIterableEquals(List.of(1, 2, 3), o.li1);
        assertEquals(new SUB(1, null), o.li2.get(0));
        assertEquals(new SUB(2, new SUB(21, null)), o.li2.get(1));
        assertEquals(new SUB(3, new SUB(31, null)), o.li2.get(2));
        assertEquals(2, o.li3.size());
        assertEquals(HashSet.class, o.li3.get(0).getClass());
        assertEquals(HashSet.class, o.li3.get(1).getClass());
        assertEquals(1, o.li3.get(0).size());
        assertEquals(1, o.li3.get(1).size());
        assertTrue(o.li3.get(0).contains(new SUB(1, null)));
        assertTrue(o.li3.get(1).contains(new SUB(2, null)));
        assertEquals(Set.of((short) 1, (short) 3), o.ss);
        assertEquals(3, o.ma.size());
        assertEquals(true, o.ma.get("yes"));
        assertEquals(true, o.ma.get("sure"));
        assertEquals(false, o.ma.get("nope"));

        assertNotNull(o.listOfAbstract);
        assertEquals(2, o.listOfAbstract.size());
        assertTrue(o.listOfAbstract.get(0) instanceof XXX);
        assertTrue(o.listOfAbstract.get(1) instanceof YYY);
        assertEquals(4711, o.listOfAbstract.get(0).shared_field);
        assertEquals(4712, o.listOfAbstract.get(1).shared_field);
        assertEquals(1111, ((XXX) o.listOfAbstract.get(0)).field);
        assertEquals(2222, ((YYY) o.listOfAbstract.get(1)).field);

        String json_2 = toJson(o);
        String json_3 = toJson(fromJson(AAA.class, json_2));

        assertEquals(json_2, json_3);
    }

    //############################################################################################################################################################
    //############################################################################################################################################################
    //############################################################################################################################################################
    public static class SUB {
        int id;
        SUB sub;

        @SuppressWarnings("unused")
        public SUB() {
        }

        public SUB(int id, SUB sub) {
            this.id  = id;
            this.sub = sub;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            SUB sub1 = (SUB) o;

            if (id != sub1.id) {
                return false;
            }
            return sub != null ? sub.equals(sub1.sub) : sub1.sub == null;
        }

        @Override
        public int hashCode() {
            int result = id;
            result = 31 * result + (sub != null ? sub.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "SUB[" + id + (sub == null ? "" : ", " + sub) + "]";
        }
    }

    public static class NUM {
        int num;

        public NUM(int num) {
            this.num = num;
        }
    }

    public static class BOOL {
        boolean bool;

        public BOOL(boolean bool) {
            this.bool = bool;
        }
    }

    public static class STRING {
        String string;

        public STRING(String string) {
            this.string = string;
        }
    }

    @SuppressWarnings("unused")
    public static abstract class Base {
        String $type;

        @JsonClassSelector
        public static Class<?> selectClassFrom(String name, Object value) throws ClassNotFoundException {
            if (!name.equals("$type")) {
                throw new ClassNotFoundException("json class not found: '$type' field expected as selector field name but found " + name + " (value=" + value + ")");
            }
            if (!(value instanceof String)) {
                throw new ClassNotFoundException("json class not found: '$type' field should be String but is " + (value == null ? "<null>" : value.getClass().getSimpleName()) + " (value=" + value + ")");
            }
            return Class.forName(((String) value).replaceAll("#.*", ""));
        }
    }

    public static abstract class Abstract extends Base {
        int shared_field;
    }

    public static class XXX extends Abstract {
        int field;
    }

    public static class YYY extends Abstract {
        int field;
    }

    public static class AAA {
        NUM    num;
        BOOL   bool;
        STRING string;
        @JsonName("b-o")
        boolean bo;
        @JsonName("b_y+t#e")
        byte    by;
        short  sh;
        int    in;
        long   lo;
        double db;
        float  fl;
        String st;
        char   ch;
        SUB    ob1;
        SUB    ob2;

        List<Integer>        li1;
        List<SUB>            li2;
        List<Set<SUB>>       li3;
        Set<Short>           ss;
        Map<String, Boolean> ma;
        List<Abstract>       listOfAbstract;
        // TODO: Abstract[]           arrayOfAbstract;
    }

    public List<Serializable> getTestObject1() {
        return new ArrayList<>(
                Arrays.asList(
                        1L,
                        2L,
                        3L,
                        new ArrayList<>(Arrays.asList(
                                1L,
                                2L,
                                3L,
                                new ArrayList<>(Arrays.asList(
                                        1L,
                                        2L,
                                        3L
                                ))
                        ))
                ));
    }

    private static Map<Object, Object> makeMap(Entry<?, ?>... entries) {
        Map<Object, Object> map = new HashMap<>();
        for (Entry<?, ?> entry : entries) {
            if (map.put(entry.getKey(), entry.getValue()) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }
        return map;
    }

    private static String quoted(String s) {
        return '"' + s + '"';
    }
}
