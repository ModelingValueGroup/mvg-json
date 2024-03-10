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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.modelingvalue.json.IdTests.A.Fingerprinter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IdTests {
    private static final String EXPECTED_ID_GETTERS = "{\"id\":123,\"children\":[{\"id\":678,\"children\":null},{\"id\":123},{\"id\":345,\"children\":null}]}";
    private static final String EXPECTED_MINIMAL    = /**/
            /**/"{" +
            /**//**/"\"id\":\"xxx\"," +
            /**//**/"\"children\":[]," +
            /**//**/"\"friend\":{\"id\":\"xxx\"}," +
            /**//**/"\"parent\":null" +
            /**/"}";
    private static final String MINIMAL_OK1         = /**/
            /**/"{" +
            /**//**/"\"id\":\"xxx\"," +
            /**//**/"\"friend\":{\"id\":\"yyy\", \"friend\": {\"id\":\"yyy\"}}" +
            /**/"}";
    private static final String MINIMAL_OK2         = /**/
            /**/"{" +
            /**//**/"\"id\":\"xxx\"," +
            /**//**/"\"friend\":{\"id\":\"yyy\", \"friend\": {\"id\":\"zzz\"}}" +
            /**/"}";
    private static final String MINIMAL_BAD1        = /**/
            /**/"{" +
            /**//**/"\"id\":\"xxx\"," +
            /**//**/"\"friend\":{\"id\":\"xxx\", \"friend\": {\"id\":\"yyy\"}}" +
            /**/"}";
    private static final String MINIMAL_BAD2        = /**/
            /**/"{" +
            /**//**/"\"id\":\"xxx\"," +
            /**//**/"\"friend\":{\"friend\": {\"id\":\"yyy\"},\"id\":\"xxx\"}" +
            /**/"}";
    private static final String EXPECTED_SHARED     = /**/
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
    private static final String EXPECTED_CYCLIC     = /**/
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
    public void idGettersTest() {
        C testObject = C.demo();

        String rendered = new ToJson(testObject).render();
        Assertions.assertEquals(EXPECTED_ID_GETTERS, rendered);

        C parsed = FromJsonGeneric.fromJson(C.class, rendered);
        Assertions.assertEquals(123, parsed.getId());
        Assertions.assertEquals(678, parsed.getChildren().get(0).getId());
        Assertions.assertEquals(123, parsed.getChildren().get(1).getId());
        Assertions.assertEquals(parsed, parsed.getChildren().get(1));
        Assertions.assertEquals(123, parsed.getChildren().get(1).getId());
        Assertions.assertEquals(345, parsed.getChildren().get(2).getId());
    }

    @Test
    public void minimalTest() {
        A testObject = A.makeMinimalModel();

        String rendered = new ToJson(testObject).render();
        Assertions.assertEquals(EXPECTED_MINIMAL, rendered);

        A parsed = FromJsonGeneric.fromJson(A.class, rendered);
        Assertions.assertEquals(testObject.fingerprint(new Fingerprinter()), parsed.fingerprint(new Fingerprinter()));
    }

    @Test
    public void minimalBTest() throws NoSuchFieldException {
        B testObject = B.makeMinimalModel();

        Assertions.assertThrows(StackOverflowError.class, () -> new ToJson(testObject).render());

        Config config = new Config();
        config.addJsonIdAnnotation(B.class.getDeclaredField("id"));
        String rendered = new ToJson(testObject, config).render();
        Assertions.assertEquals(EXPECTED_MINIMAL, rendered);

        B parsed = FromJsonGeneric.fromJson(B.class, rendered);
        Assertions.assertEquals(testObject.fingerprint(new B.Fingerprinter()), parsed.fingerprint(new B.Fingerprinter()));
    }

    @Test
    public void ok1Test() {
        FromJsonGeneric.fromJson(A.class, MINIMAL_OK1);
    }

    @Test
    public void ok2Test() {
        FromJsonGeneric.fromJson(A.class, MINIMAL_OK2);
    }

    @Test
    public void bad1Test() {
        IllegalArgumentException iae = Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> FromJsonGeneric.fromJson(A.class, MINIMAL_BAD1));
        Assertions.assertEquals("json syntax error: id references must be the only field set when referencing a previous object (at 44: [d\":\"xxx\", \"friend\": <{>\"id\":\"yyy\"}}}])", iae.getMessage());
    }

    @Test
    public void bad2Test() {
        IllegalArgumentException iae = Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> FromJsonGeneric.fromJson(A.class, MINIMAL_BAD2));
        Assertions.assertEquals("json syntax error: id references must be the only field present when referencing a previous object: found id: xxx (at 55: [d\":\"yyy\"},\"id\":\"xxx\"<}>}])", iae.getMessage());
    }

    @Test
    public void sharedTest() {
        A testObject = A.makeSharedModel();

        String rendered = new ToJson(testObject).render();
        Assertions.assertEquals(EXPECTED_SHARED, rendered);

        A parsed = FromJsonGeneric.fromJson(A.class, rendered);
        Assertions.assertEquals(testObject.fingerprint(new Fingerprinter()), parsed.fingerprint(new Fingerprinter()));
    }

    @Test
    public void cyclicTest() {
        A testObject = A.makeCyclicModel();

        String rendered = new ToJson(testObject).render();
        Assertions.assertEquals(EXPECTED_CYCLIC, rendered);

        A parsed = FromJsonGeneric.fromJson(A.class, rendered);
        Assertions.assertEquals(testObject.fingerprint(new Fingerprinter()), parsed.fingerprint(new Fingerprinter()));

        parsed.children.get(0).children.get(2).friend = new A("renee");
        Assertions.assertNotEquals(testObject.fingerprint(new Fingerprinter()), parsed.fingerprint(new Fingerprinter()));

    }

    @SuppressWarnings("CanBeFinal")
    public static class A {
        @JsonId
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

        private static A makeMinimalModel() {
            A xxx = new A("xxx");
            xxx.friend = xxx;
            return xxx;
        }

        private static A makeSharedModel() {
            A dad   = new A("dad");
            A child = new A("child");

            dad.addChild(child);
            dad.friend   = child;
            child.friend = dad;

            return dad;
        }

        private static A makeCyclicModel() {
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

        public static class Fingerprinter {
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
    }

    @SuppressWarnings("CanBeFinal")
    public static class B {
        String  id;
        B       parent;
        List<B> children = new ArrayList<>();
        B       friend;

        @SuppressWarnings("unused")
        public B() {
        }

        public B(String id) {
            this.id = id;
        }

        private static B makeMinimalModel() {
            B xxx = new B("xxx");
            xxx.friend = xxx;
            return xxx;
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

        public static class Fingerprinter {
            int             num;
            Map<B, Integer> a2num = new HashMap<>();
            Map<Integer, B> num2a = new HashMap<>();

            private void add(B o) {
                num2a.computeIfAbsent(a2num.computeIfAbsent(o, __1 -> num++), __ -> o);
            }

            void add(B... oo) {
                for (B o : oo) {
                    add(o);
                }
            }

            void add(List<B> l) {
                for (B o : l) {
                    add(o);
                }
            }

            public String get() {
                return num2a.keySet().stream().sorted().map(n -> String.format("%03d_%s", n, num2a.get(n).id)).collect(Collectors.joining("\n"));
            }
        }
    }

    @SuppressWarnings("unused")
    public static class C {
        private int     id;
        private List<C> children;

        @JsonId
        public int getId() {
            return id;
        }

        @JsonId
        public void setId(int id) {
            this.id = id;
        }

        public List<C> getChildren() {
            return children;
        }

        public void setChildren(List<C> children) {
            this.children = children;
        }

        public static C demo() {
            C root = new C();
            root.id = 123;
            C child1 = new C();
            child1.id = 678;
            C child2 = new C();
            child2.id     = 345;
            root.children = List.of(child1, root, child2);
            return root;
        }
    }

}
