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

import org.modelingvalue.json.GenericsUtil.TypeInfo;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class FromJsonGeneric extends FromJsonBase<Object, Object> {
    @SuppressWarnings("unchecked")
    public static <T> T fromJson(Type t, String s) {
        return (T) new FromJsonGeneric(t, s).parse();
    }

    private final Stack<TypeInfo>     typeInfoStack = new Stack<>();
    private final Map<Type, TypeInfo> typeInfoMap   = new HashMap<>();

    public FromJsonGeneric(Type t, String input) {
        super(input);
        TypeInfo typeInfo = GenericsUtil.makeTypeInfo(t);
        typeInfoMap.put(t, typeInfo);
        typeInfoStack.push(typeInfo);
    }

    private TypeInfo addToTypeInfoMap(Type type) {
        typeInfoMap.computeIfAbsent(type, GenericsUtil::makeTypeInfo);
        return typeInfoMap.get(type);
    }

    ///////////////////////////////////////
    @Override
    protected Object makeMap() {
        Stack<Object> path = getPath();
        if (!path.isEmpty()) {
            TypeInfo ti = addToTypeInfoMap(typeInfoStack.peek().getSubType(path.peek()));
            typeInfoStack.push(ti);
        }
        return typeInfoStack.peek().getConstructor().construct();
    }

    @Override
    protected Object makeMapKey(String key) {
        return key;
    }

    @Override
    protected Object makeMapEntry(Object m, Object key, Object value) {
        typeInfoStack.peek().getSubSetter(key).set(m, key, value);
        return m;
    }

    @Override
    protected Object closeMap(Object m) {
        typeInfoStack.pop();
        return m;
    }

    ///////////////////////////////////////
    @Override
    protected Object makeArray() {
        Stack<Object> path = getPath();
        if (!path.isEmpty()) {
            TypeInfo ti = addToTypeInfoMap(typeInfoStack.peek().getSubType(path.peek()));
            typeInfoStack.push(ti);
        }
        return typeInfoStack.peek().getConstructor().construct();
    }

    @Override
    protected Object makeArrayEntry(Object a, Object value) {
        int index = getIndex();
        typeInfoStack.peek().getSubSetter(index).set(a, index, value);
        return a;
    }

    @Override
    protected Object closeArray(Object l) {
        typeInfoStack.pop();
        return l;
    }
}
