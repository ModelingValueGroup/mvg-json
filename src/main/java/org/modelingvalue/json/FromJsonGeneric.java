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

import org.modelingvalue.json.GenericsUtil.SubSetter;
import org.modelingvalue.json.GenericsUtil.TypeInfo;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;

import static org.modelingvalue.json.FromJsonGeneric.IdAcceptState.MAY_BE_ID;
import static org.modelingvalue.json.FromJsonGeneric.IdAcceptState.MAY_BE_MORE;
import static org.modelingvalue.json.FromJsonGeneric.IdAcceptState.MAY_NOT_BE_MORE;

public class FromJsonGeneric extends FromJsonBase<Object, Object> {
    @SuppressWarnings("unchecked")
    public static <T> T fromJson(Type t, String s) {
        return (T) new FromJsonGeneric(t, s).parse();
    }

    private final Stack<TypeInfo>     typeInfoStack    = new Stack<>();
    private final Map<Type, TypeInfo> typeInfoMap      = new HashMap<>();
    private final Consumer<TypeInfo>  topStackReplacer = replacement -> {
        typeInfoStack.pop();
        typeInfoStack.push(replacement);
    };
    private final Map<Object, Object> id2objectMap     = new HashMap<>();
    private       boolean             ignoreSFOs;

    enum IdAcceptState {
        MAY_BE_MORE, MAY_BE_ID, MAY_NOT_BE_MORE
    }

    private IdAcceptState idAcceptState = MAY_BE_MORE;

    public FromJsonGeneric(Type t, String input) {
        super(input);
        pushType(t);
    }

    @SuppressWarnings("unused")
    public void setIgnoreSFOs(boolean b) {
        ignoreSFOs = b;
    }

    private void pushType(Type subType) {
        TypeInfo typeInfo = typeInfoMap.computeIfAbsent(subType, t -> GenericsUtil.makeTypeInfo(t, ignoreSFOs, topStackReplacer));
        typeInfoStack.push(typeInfo);
    }

    private Object makeObject() {
        Stack<Object> path = getPath();
        if (!path.isEmpty()) {
            Type subType = typeInfoStack.peek().getSubType(path.peek());
            pushType(subType);
        }
        return typeInfoStack.peek().getMaker().make();
    }

    private Object closeObject(Object m) {
        typeInfoStack.pop();
        return m;
    }

    ///////////////////////////////////////
    @Override
    protected Object makeMap() {
        if (idAcceptState == MAY_NOT_BE_MORE) {
            throw error("id references must be the only field set when referencing a previous object");
        }
        idAcceptState = MAY_BE_ID;
        return makeObject();
    }

    @Override
    protected Object makeMapEntry(Object m, Object key, Object value) {
        TypeInfo typeInfo = typeInfoStack.peek();
        switch (idAcceptState) {
        case MAY_BE_ID:
            idAcceptState = MAY_BE_MORE;
            if (typeInfo.isIdField(key.toString())) {
                final Object mm = m;
                m = id2objectMap.computeIfAbsent(value, __ -> mm);
                if (m != mm) {
                    idAcceptState = MAY_NOT_BE_MORE;
                }
            }
            break;
        case MAY_BE_MORE:
            if (typeInfo.isIdField(key.toString())) {
                final Object mm = m;
                m = id2objectMap.computeIfAbsent(value, __ -> mm);
                if (m != mm) {
                    throw error("id references must be the only field present when referencing a previous object: found " + key + ": " + value);
                }
            }
            break;
        case MAY_NOT_BE_MORE:
            throw error("id references must be the only field present when referencing a previous object: found " + key + ": " + value);
        }
        SubSetter subSetter = typeInfo.getSubSetter(key);
        return subSetter.set(m, key, value);
    }

    @Override
    protected Object closeMap(Object m) {
        idAcceptState = MAY_BE_MORE;
        return closeObject(m);
    }
    ///////////////////////////////////////

    @Override
    protected Object makeArray() {
        return makeObject();
    }

    @Override
    protected Object makeArrayEntry(Object a, int index, Object value) {
        SubSetter subSetter = typeInfoStack.peek().getSubSetter(index);
        return subSetter.set(a, index, value);
    }

    @Override
    protected Object closeArray(Object l) {
        return closeObject(l);
    }
}
