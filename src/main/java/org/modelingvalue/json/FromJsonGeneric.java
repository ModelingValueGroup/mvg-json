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

import org.modelingvalue.json.GenericsUtil.Maker;
import org.modelingvalue.json.GenericsUtil.SubSetter;
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
    private       boolean             ignoreSFOs;

    public FromJsonGeneric(Type t, String input) {
        super(input);
        pushType(t);
    }

    @SuppressWarnings("unused")
    public void setIgnoreSFOs(boolean b) {
        ignoreSFOs = b;
    }

    private void pushType(Type subType) {
        TypeInfo typeInfo = typeInfoMap.computeIfAbsent(subType, t -> GenericsUtil.makeTypeInfo(t, ignoreSFOs));
        typeInfoStack.push(typeInfo);
    }

    private Object makeObject() {
        Stack<Object> path = getPath();
        if (!path.isEmpty()) {
            Type subType = typeInfoStack.peek().getSubType(path.peek());
            pushType(subType);
        }
        Maker maker = typeInfoStack.peek().getMaker();
        return maker.make();
    }

    private Object makeEntry(Object a, Object value, Object index) {
        SubSetter subSetter = typeInfoStack.peek().getSubSetter(index);
        return subSetter.set(a, index, value);
    }

    private Object closeObject(Object m) {
        typeInfoStack.pop();
        return m;
    }

    ///////////////////////////////////////
    @Override
    protected Object makeMap() {
        return makeObject();
    }

    @Override
    protected Object makeMapEntry(Object m, Object key, Object value) {
        return makeEntry(m, value, key);
    }

    @Override
    protected Object closeMap(Object m) {
        return closeObject(m);
    }
    ///////////////////////////////////////

    @Override
    protected Object makeArray() {
        return makeObject();
    }

    @Override
    protected Object makeArrayEntry(Object a, int index, Object value) {
        return makeEntry(a, value, index);
    }

    @Override
    protected Object closeArray(Object l) {
        return closeObject(l);
    }
}
