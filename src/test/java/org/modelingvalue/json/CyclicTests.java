package org.modelingvalue.json;

import org.junit.jupiter.api.Test;

import java.util.List;

public class CyclicTests {
    @Test
    public void cyclicTest() {
        Object cyclic = makeCyclicModel();
        ToJson toJson = new ToJson(cyclic);
        toJson.setIdFunction(o -> {
            if (o instanceof AAA) {
                return ((AAA) o).id;
            } else if (o instanceof BBB) {
                return ((BBB) o).id;
            } else {
                return null;
            }
        });
        String s = toJson.render();
        System.err.println("    " + s);
    }

    public static class AAA {
        String    id;
        List<BBB> bbbs;
    }

    public static class BBB {
        String id;
        AAA    aaa;
    }

    private Object makeCyclicModel() {
        AAA a  = new AAA();
        BBB b1 = new BBB();
        BBB b2 = new BBB();

        a.id  = "ID-a";
        b1.id = "ID-b1";
        b2.id = "ID-b2";

        a.bbbs = List.of(b1, b2, b1, b2);
        b1.aaa = a;
        b2.aaa = a;

        return a;
    }
}
