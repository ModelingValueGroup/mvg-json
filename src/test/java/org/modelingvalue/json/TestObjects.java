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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class TestObjects {
    static Map<Object, Object> makeMap(Entry<?, ?>... entries) {
        Map<Object, Object> map = new HashMap<>();
        for (Entry<?, ?> entry : entries) {
            if (map.put(entry.getKey(), entry.getValue()) != null) {
                throw new IllegalStateException("Duplicate key");
            }
        }
        return map;
    }

    static String quoted(String s) {
        return '"' + s + '"';
    }

    public static List<Serializable> getTestObject1() {
        return new ArrayList<>(
                List.of(
                        1L,
                        2L,
                        new HashMap<>(Map.of("three", 3L)),
                        new ArrayList<>(List.of(
                                new ArrayList<>(List.of(
                                        1L,
                                        2L,
                                        3L
                                ))
                        ))
                ));
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
            return Objects.equals(sub, sub1.sub);
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

    @SuppressWarnings("CanBeFinal")
    public static class NUM {
        int num;

        public NUM(int num) {
            this.num = num;
        }
    }

    @SuppressWarnings("CanBeFinal")
    public static class BOOL {
        boolean bool;

        public BOOL(boolean bool) {
            this.bool = bool;
        }
    }

    @SuppressWarnings("CanBeFinal")
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
        @SuppressWarnings("unused")
        Abstract subAbstract;
    }

    public static class YYY extends Abstract {
        int field;
    }

    @SuppressWarnings("unused")
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
        Abstract             anAbstract;
        //TODO
        // Abstract[]           arrayOfAbstract;
    }
}
