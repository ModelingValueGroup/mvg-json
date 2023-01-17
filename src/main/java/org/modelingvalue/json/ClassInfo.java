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

import org.modelingvalue.json.U.RW;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

class ClassInfo {
    private abstract static class MemberInfo {
        private final String name;

        private MemberInfo(String name) {
            this.name = name;
        }

        abstract Function<Object, Object> getAccessor();

        abstract boolean isId();

        Entry<Object, Object> getEntry(Object o) {
            return new SimpleEntry<>(name, getAccessor().apply(o));
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private class FieldMemberInfo extends MemberInfo {
        private final Field f;

        private FieldMemberInfo(Field f) {
            super(U.fieldToElementName(f, config));
            this.f = f;
        }

        @Override
        Function<Object, Object> getAccessor() {
            return o -> {
                try {
                    return f.get(o);
                } catch (IllegalAccessException e) {
                    // just ignore
                    return null;
                }
            };
        }

        @Override
        boolean isId() {
            return config.getAnnotation(f, JsonId.class) != null;
        }
    }

    private class MethodMemberInfo extends MemberInfo {
        private final Method m;

        private MethodMemberInfo(Method m) {
            super(U.methodToElementName(m));
            this.m = m;
        }

        @Override
        Function<Object, Object> getAccessor() {
            return o -> {
                try {
                    return m.invoke(o);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    // just ignore
                    return null;
                }
            };
        }

        @Override
        boolean isId() {
            return config.getAnnotation(m, JsonId.class) != null;
        }
    }

    private final Class<?>            clazz;
    private final Config              config;
    private final List<MemberInfo>    members       = new ArrayList<>();
    private final MemberInfo          idField;
    private final Map<Object, String> seenBeforeMap = new HashMap<>();

    ClassInfo(Class<?> clazz, Config config) {
        this.clazz  = clazz;
        this.config = config;

        U.findElements(clazz, config, m -> members.add(new MethodMemberInfo(m)), f -> members.add(new FieldMemberInfo(f)), RW.READ);
        members.sort(Comparator.comparing(m -> m.name));
        idField = members.stream().filter(MemberInfo::isId).findFirst().orElse(null);
        if (idField != null) {
            members.remove(idField);
            members.add(0, idField);
        }
    }

    private String seenBeforeId(Object o) {
        return idField != null ? idField.getAccessor().apply(o).toString() : null;
    }

    private boolean seenBefore(Object o) {
        if (idField != null) {
            if (seenBeforeMap.containsKey(o)) {
                return true;
            }
            seenBeforeMap.put(o, seenBeforeId(o));
        }
        return false;
    }

    public Iterator<Entry<Object, Object>> getIntrospectionIterator(Object o) {
        if (seenBefore(o)) {
            return Stream.of(idField.getEntry(o)).iterator();
        } else {
            Stream<SimpleEntry<Object, Object>> classStream = config.includeClassNameInIntrospection ? Stream.of(new SimpleEntry<>(U.CLASS_NAME_FIELD_NAME, clazz.getName())) : Stream.empty();
            return Stream.concat(classStream, members.stream().map(m -> m.getEntry(o))).iterator();
        }
    }
}
