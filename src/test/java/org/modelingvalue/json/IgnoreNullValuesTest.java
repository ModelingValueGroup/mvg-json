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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IgnoreNullValuesTest {
    public static final String EXP_1 = """
                                       {
                                         "id": 1,
                                         "list": [
                                           "A",
                                           null,
                                           "C",
                                           null
                                         ],
                                         "map": {
                                           "a": "A",
                                           "b": null,
                                           "c": "C"
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
                                           "A",
                                           null,
                                           "C"
                                         ],
                                         "map": {
                                           "a": "A",
                                           "b": null,
                                           "c": "C"
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
