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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IdTests {
    private static final String EXPECTED_MINIMAL = /**/
            /**/"{" +
            /**//**/"\"id\":\"xxx\"," +
            /**//**/"\"children\":[]," +
            /**//**/"\"friend\":{\"id\":\"xxx\"}," +
            /**//**/"\"parent\":null" +
            /**/"}";
    private static final String MINIMAL_OK1      = /**/
            /**/"{" +
            /**//**/"\"id\":\"xxx\"," +
            /**//**/"\"friend\":{\"id\":\"yyy\", \"friend\": {\"id\":\"yyy\"}}" +
            /**/"}";
    private static final String MINIMAL_OK2      = /**/
            /**/"{" +
            /**//**/"\"id\":\"xxx\"," +
            /**//**/"\"friend\":{\"id\":\"yyy\", \"friend\": {\"id\":\"zzz\"}}" +
            /**/"}";
    private static final String MINIMAL_BAD1     = /**/
            /**/"{" +
            /**//**/"\"id\":\"xxx\"," +
            /**//**/"\"friend\":{\"id\":\"xxx\", \"friend\": {\"id\":\"yyy\"}}" +
            /**/"}";
    private static final String MINIMAL_BAD2     = /**/
            /**/"{" +
            /**//**/"\"id\":\"xxx\"," +
            /**//**/"\"friend\":{\"friend\": {\"id\":\"yyy\"},\"id\":\"xxx\"}" +
            /**/"}";
    private static final String EXPECTED_SHARED  = /**/
            /**/"{" +
            /**//**/"\"id\":\"dad\"," +
            /**//**/"\"children\":" +
            /**//**/"[" +
            /**//**//**/"{" +
            /**//**//**//**/"\"id\":\"child\"," +
            /**//**//**//**/"\"children\":[]," +
            /**//**//**//**/"\"friend\":{\"id\":\"dad\"}," +
            /**//**//**//**/"\"parent\":{\"id\":\"dad\"}" +
            /**//**//**/"}" +
            /**//**/"]," +
            /**//**/"\"friend\":{\"id\":\"child\"}," +
            /**//**/"\"parent\":null" +
            /**/"}";
    private static final String EXPECTED_CYCLIC  = /**/
            /**/"{" +
            /**//**/"\"id\":\"jaap\"," +
            /**//**/"\"children\":" +
            /**//**/"[" +
            /**//**//**/"{" +
            /**//**//**//**/"\"id\":\"frits\"," +
            /**//**//**//**/"\"children\":" +
            /**//**//**//**/"[" +
            /**//**//**//**//**/"{" +
            /**//**//**//**//**//**/"\"id\":\"boris\"," +
            /**//**//**//**//**//**/"\"children\":[]," +
            /**//**//**//**//**//**/"\"friend\":{\"id\":\"jaap\"}," +
            /**//**//**//**//**//**/"\"parent\":{\"id\":\"frits\"}" +
            /**//**//**//**//**/"}," +
            /**//**//**//**//**/"{" +
            /**//**//**//**//**//**/"\"id\":\"renee\"," +
            /**//**//**//**//**//**/"\"children\":[]," +
            /**//**//**//**//**//**/"\"friend\":null," +
            /**//**//**//**//**//**/"\"parent\":{\"id\":\"frits\"}" +
            /**//**//**//**//**/"}," +
            /**//**//**//**//**/"{" +
            /**//**//**//**//**//**/"\"id\":\"evert\"," +
            /**//**//**//**//**//**/"\"children\":[]," +
            /**//**//**//**//**//**/"\"friend\":{\"id\":\"renee\"}," +
            /**//**//**//**//**//**/"\"parent\":{\"id\":\"frits\"}" +
            /**//**//**//**//**/"}" +
            /**//**//**//**/"]," +
            /**//**//**//**/"\"friend\":{\"id\":\"jaap\"}," +
            /**//**//**//**/"\"parent\":{\"id\":\"jaap\"}" +
            /**//**//**/"}," +
            /**//**//**/"{" +
            /**//**//**//**/"\"id\":\"emma\"," +
            /**//**//**//**/"\"children\":[]," +
            /**//**//**//**/"\"friend\":{\"id\":\"frits\"}," +
            /**//**//**//**/"\"parent\":{\"id\":\"jaap\"}" +
            /**//**//**/"}" +
            /**//**/"]," +
            /**//**/"\"friend\":{\"id\":\"emma\"}," +
            /**//**/"\"parent\":null" +
            /**/"}";

    @Test
    public void minimalTest() {
        A testObject = makeMinimalModel();

        String rendered = new ToJson(testObject).withIncludeIdInIntrospection(true).render();
        Assertions.assertEquals(EXPECTED_MINIMAL, rendered);

        A parsed = (A) new FromJsonGeneric(A.class, rendered).parse();
        Assertions.assertEquals(testObject.fingerprint(new Fingerprinter()), parsed.fingerprint(new Fingerprinter()));
    }

    @Test
    public void ok1Test() {
        new FromJsonGeneric(A.class, MINIMAL_OK1).parse();
    }

    @Test
    public void ok2Test() {
        new FromJsonGeneric(A.class, MINIMAL_OK2).parse();
    }

    @Test
    public void bad1Test() {
        IllegalArgumentException iae = Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> new FromJsonGeneric(A.class, MINIMAL_BAD1).parse());
        Assertions.assertEquals("json syntax error: id references must be the only field set when referencing a previous object (at 44: [d\":\"xxx\", \"friend\": <{>\"id\":\"yyy\"}}}])", iae.getMessage());
    }

    @Test
    public void bad2Test() {
        IllegalArgumentException iae = Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> new FromJsonGeneric(A.class, MINIMAL_BAD2).parse());
        Assertions.assertEquals("json syntax error: id references must be the only field present when referencing a previous object: found id: xxx (at 55: [d\":\"yyy\"},\"id\":\"xxx\"<}>}])", iae.getMessage());
    }

    @Test
    public void sharedTest() {
        A testObject = makeSharedModel();

        String rendered = new ToJson(testObject).withIncludeIdInIntrospection(true).render();
        Assertions.assertEquals(EXPECTED_SHARED, rendered);

        A parsed = (A) new FromJsonGeneric(A.class, rendered).parse();
        Assertions.assertEquals(testObject.fingerprint(new Fingerprinter()), parsed.fingerprint(new Fingerprinter()));
    }

    @Test
    public void cyclicTest() {
        A testObject = makeCyclicModel();

        String rendered = new ToJson(testObject).withIncludeIdInIntrospection(true).render();
        Assertions.assertEquals(EXPECTED_CYCLIC, rendered);

        A parsed = (A) new FromJsonGeneric(A.class, rendered).parse();
        Assertions.assertEquals(testObject.fingerprint(new Fingerprinter()), parsed.fingerprint(new Fingerprinter()));

        parsed.children.get(0).children.get(2).friend = new A("renee");
        Assertions.assertNotEquals(testObject.fingerprint(new Fingerprinter()), parsed.fingerprint(new Fingerprinter()));

    }

    public static class A {
        @JsonIsId
        String id;
        A       parent;
        List<A> children = new ArrayList<>();
        A       friend;

        @SuppressWarnings("unused")
        public A() {
        }

        public A(String id) {
            this.id = id;
        }

        public void addChild(A child) {
            children.add(child);
            child.parent = this;
        }

        @Override
        public String toString() {
            return id + children.stream().map(a -> a.id).collect(Collectors.toList()) + (friend == null ? "<null>" : friend.id);
        }

        public String fingerprint(Fingerprinter fp) {
            fp.add(parent, friend);
            fp.add(children);
            children.forEach(c -> c.fingerprint(fp));
            return fp.get();
        }
    }

    private static class Fingerprinter {
        int             num;
        Map<A, Integer> a2num = new HashMap<>();
        Map<Integer, A> num2a = new HashMap<>();

        private void add(A o) {
            num2a.computeIfAbsent(a2num.computeIfAbsent(o, __1 -> num++), __ -> o);
        }

        void add(A... oo) {
            for (A o : oo) {
                add(o);
            }
        }

        void add(List<A> l) {
            for (A o : l) {
                add(o);
            }
        }

        public String get() {
            return num2a.keySet().stream().sorted().map(n -> String.format("%03d_%s", n, num2a.get(n).id)).collect(Collectors.joining("\n"));
        }
    }

    private A makeMinimalModel() {
        A xxx = new A("xxx");
        xxx.friend = xxx;
        return xxx;
    }

    private A makeSharedModel() {
        A dad   = new A("dad");
        A child = new A("child");

        dad.addChild(child);
        dad.friend   = child;
        child.friend = dad;

        return dad;
    }

    private A makeCyclicModel() {
        A a  = new A("jaap");
        A b1 = new A("frits");
        A b2 = new A("emma");
        A c1 = new A("boris");
        A c2 = new A("renee");
        A c3 = new A("evert");

        a.addChild(b1);
        a.addChild(b2);
        b1.addChild(c1);
        b1.addChild(c2);
        b1.addChild(c3);

        a.friend  = b2;
        b1.friend = a;
        b2.friend = b1;
        c1.friend = a;
        c3.friend = c2;

        return a;
    }
}
