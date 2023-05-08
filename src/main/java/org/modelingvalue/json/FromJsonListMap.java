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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FromJsonListMap extends FromJsonBase<Iterable<Object>, Map<String, Object>> {
    public static Object fromJson(String s) {
        return fromJson(s, new Config());
    }

    public static Object fromJson(String s, Config config) {
        return new FromJsonListMap(s, config).parse();
    }

    protected FromJsonListMap(String input, Config config) {
        super(input, config);
    }

    @Override
    protected HashMap<String, Object> makeMap() {
        return new HashMap<>();
    }

    @Override
    protected Iterable<Object> makeArray() {
        return new ArrayList<>();
    }

    @Override
    protected Map<String, Object> makeMapEntry(Map<String, Object> m, Object key, Object value) {
        m.put(key == null ? null : key.toString(), value);
        return m;
    }

    @Override
    protected Iterable<Object> makeArrayEntry(Iterable<Object> l, int index, Object o) {
        ((List<Object>) l).add(o);
        return l;
    }
}
