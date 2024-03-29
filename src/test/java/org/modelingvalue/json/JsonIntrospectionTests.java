//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2023 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Objects;

import static org.modelingvalue.json.Json.fromJson;
import static org.modelingvalue.json.Json.toJson;

public class JsonIntrospectionTests {

    @SuppressWarnings({"unused", "FieldMayBeFinal", "ConstantValue"})
    public static class ProbeA {
        int aap01 = 1;
        private                int aap02 = 2;
        protected              int aap03 = 3;
        public                 int aap04 = 4;
        //
        static                 int aap05 = 5;
        static private         int aap06 = 6;
        static protected       int aap07 = 7;
        static public          int aap08 = 8;
        //
        final                  int aap09 = 9;
        final private          int aap10 = 10;
        final protected        int aap11 = 11;
        final public           int aap12 = 12;
        //
        final static           int aap13 = 13;
        final static private   int aap14 = 14;
        final static protected int aap15 = 15;
        final static public    int aap16 = 15;

        int aap99 = 99;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ProbeA probeA = (ProbeA) o;
            return aap01 == probeA.aap01 && aap02 == probeA.aap02 && aap03 == probeA.aap03 && aap04 == probeA.aap04 && aap09 == probeA.aap09 && aap10 == probeA.aap10 && aap11 == probeA.aap11 && aap12 == probeA.aap12 && aap99 == probeA.aap99;
        }

        @Override
        public int hashCode() {
            return Objects.hash(aap01, aap02, aap03, aap04, aap09, aap10, aap11, aap12, aap99);
        }
    }

    @SuppressWarnings({"unused", "FieldMayBeFinal", "ConstantValue"})
    public static class ProbeB extends ProbeA {
        int aap01 = 101;
        private                int aap02 = 102;
        protected              int aap03 = 103;
        public                 int aap04 = 104;
        //
        static                 int aap05 = 105;
        static private         int aap06 = 106;
        static protected       int aap07 = 107;
        static public          int aap08 = 108;
        //
        final                  int aap09 = 109;
        final private          int aap10 = 1010;
        final protected        int aap11 = 1011;
        final public           int aap12 = 1012;
        //
        final static           int aap13 = 1013;
        final static private   int aap14 = 1014;
        final static protected int aap15 = 1015;
        final static public    int aap16 = 1015;

        @JsonIgnore
        int aap_ignore = 666;

        ProbeA probea = new ProbeA();

        @SuppressWarnings("SameReturnValue")
        String getName() {
            return "xyzzy";
        }
        void setName(String name){
            // nop
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ProbeB probeB = (ProbeB) o;
            return aap01 == probeB.aap01 && aap02 == probeB.aap02 && aap03 == probeB.aap03 && aap04 == probeB.aap04 && aap09 == probeB.aap09 && aap10 == probeB.aap10 && aap11 == probeB.aap11 && aap12 == probeB.aap12 && aap_ignore == probeB.aap_ignore && Objects.equals(probea, probeB.probea);
        }

        @Override
        public int hashCode() {
            return Objects.hash(aap01, aap02, aap03, aap04, aap09, aap10, aap11, aap12, aap_ignore, probea);
        }
    }

    @RepeatedTest(1)
    public void listsToJson() {
        Assertions.assertEquals("{"
                                        + "\"aap01\":101,"
                                        + "\"aap02\":102,"
                                        + "\"aap03\":103,"
                                        + "\"aap04\":104,"
                                        //aap05: static field not in json
                                        //aap06: static field not in json
                                        //aap07: static field not in json
                                        //aap08: static field not in json
                                        + "\"aap09\":109,"
                                        + "\"aap10\":1010,"
                                        + "\"aap11\":1011,"
                                        + "\"aap12\":1012,"
                                        //aap13: static field not in json
                                        //aap14: static field not in json
                                        //aap15: static field not in json
                                        //aap16: static field not in json
                                        + "\"aap99\":99,"
                                        + "\"name\":\"xyzzy\","
                                        + "\"probea\":{"
                                        + /**/"\"aap01\":1,"
                                        + /**/"\"aap02\":2,"
                                        + /**/"\"aap03\":3,"
                                        + /**/"\"aap04\":4,"
                                        + /**/"\"aap09\":9,"
                                        + /**/"\"aap10\":10,"
                                        + /**/"\"aap11\":11,"
                                        + /**/"\"aap12\":12,"
                                        + /**/"\"aap99\":99"
                                        + "}"
                                        + "}", toJson(new ProbeB()));
        Assertions.assertEquals(new ProbeB(), fromJson(ProbeB.class, toJson(new ProbeB())));
    }
}
