package org.modelingvalue.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IgnoreNullValuesTest {
    public static final String EXP_1 = """
                                       {
                                         "id": 1,
                                         "list": [
                                           {"name": "A"},
                                           {"name": null},
                                           {"name": "C"},
                                           null
                                         ],
                                         "map": {
                                           "a": {"name": "A"},
                                           "b": {"name": null},
                                           "c": {"name": "C"}
                                         },
                                         "name": "name",
                                         "nullInteger": null,
                                         "nullList": null,
                                         "nullMap": null,
                                         "nullString": null
                                       }""";
    public static final String EXP_2 = """
                                       {
                                         "id": 1,
                                         "list": [
                                           {"name": "A"},
                                           {},
                                           {"name": "C"}
                                         ],
                                         "map": {
                                           "a": {"name": "A"},
                                           "b": {},
                                           "c": {"name": "C"}
                                         },
                                         "name": "name"
                                       }""";

    @Test
    public void minimalTest() {
        Config ignoringConfig = new Config();
        ignoringConfig.ignoreNullValues = true;
        Assertions.assertEquals(EXP_1, JsonPrettyfier.pretty(new ToJson(new X()).render()));
        Assertions.assertEquals(EXP_2, JsonPrettyfier.pretty(new ToJson(new X(), ignoringConfig).render()));
    }

    @SuppressWarnings("unused")
    public static class X {
        static class Y {
            public String name;

            public Y(String name) {
                this.name = name;
            }
        }

        String         nullString  = null;
        String         name        = "name";
        Integer        nullInteger = null;
        Integer        id          = 1;
        Map<String, Y> nullMap     = null;
        Map<String, Y> map         = Map.of("a", new Y("A"), "b", new Y(null), "c", new Y("C"));
        List<Y>        nullList    = null;
        List<Y>        list        = new ArrayList<>();

        {
            list.add(new Y("A"));
            list.add(new Y(null));
            list.add(new Y("C"));
            list.add(null);
        }
    }
}
