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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PropertyAccessor {
    private static final Predicate<Field> FIELD_INTROSPECTION_FILTER = //
            f -> !f.isSynthetic()//
                 && !f.isEnumConstant()//
                 && !Modifier.isStatic(f.getModifiers())//
                 && !Modifier.isVolatile(f.getModifiers())//
                 && !Modifier.isNative(f.getModifiers())//
                 && !Modifier.isTransient(f.getModifiers())//
                 && f.getAnnotation(JsonIgnore.class) == null//
                 && (Modifier.isPublic(f.getModifiers()) || !f.getDeclaringClass().getPackage().getName().startsWith("java."))//
                 && canAccess(f);

    private static final Predicate<Method> GET_METHOD_INTROSPECTION_FILTER = //
            m -> !m.isSynthetic()//
                 && m.getParameterCount() == 0//
                 && m.getReturnType() != Void.class//
                 && !m.isDefault()//
                 && !Modifier.isStatic(m.getModifiers())//
                 && !Modifier.isVolatile(m.getModifiers())//
                 && !Modifier.isNative(m.getModifiers())//
                 && !Modifier.isTransient(m.getModifiers())//
                 && m.getAnnotation(JsonIgnore.class) == null//
                 && (Modifier.isPublic(m.getModifiers()) || !m.getDeclaringClass().getPackage().getName().startsWith("java."))//
                 && m.getName().matches("^(get|is)[A-Z].*")//
                 && !m.getName().equals("getClass")//
                 && canAccess(m);

    private static final Predicate<Method> SET_METHOD_INTROSPECTION_FILTER = //
            m -> !m.isSynthetic()//
                 && m.getParameterCount() == 1//
                 && !m.isDefault()//
                 && !Modifier.isStatic(m.getModifiers())//
                 && !Modifier.isVolatile(m.getModifiers())//
                 && !Modifier.isNative(m.getModifiers())//
                 && !Modifier.isTransient(m.getModifiers())//
                 && m.getAnnotation(JsonIgnore.class) == null//
                 && (Modifier.isPublic(m.getModifiers()) || !m.getDeclaringClass().getPackage().getName().startsWith("java."))//
                 && m.getName().matches("^set[A-Z].*")//
                 && canAccess(m);

    private static boolean canAccess(Method m) {
        try {
            m.setAccessible(true);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    private static boolean canAccess(Field f) {
        try {
            f.setAccessible(true);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    static Map<String, PropertyAccessor> all(Class<?> clazz, Config config) {
        Map<String, Field>  fields  = new HashMap<>();
        Map<String, Method> getters = new HashMap<>();
        Map<String, Method> setters = new HashMap<>();

        for (Class<?> c = clazz; c != Object.class; c = c.getSuperclass()) {
            Arrays.stream(c.getDeclaredFields())//
                  .filter(FIELD_INTROSPECTION_FILTER)//
                  .forEach(f -> fields.putIfAbsent(U.getPropertyName(f), f));
            Arrays.stream(c.getDeclaredMethods())//
                  .filter(GET_METHOD_INTROSPECTION_FILTER)//
                  .forEach(m -> getters.putIfAbsent(U.getPropertyName(m), m));
            Arrays.stream(c.getDeclaredMethods())//
                  .filter(SET_METHOD_INTROSPECTION_FILTER)//
                  .forEach(m -> setters.putIfAbsent(U.getPropertyName(m), m));
        }
        return Stream.of(fields.keySet().stream(), getters.keySet().stream(), setters.keySet().stream())//
                     .flatMap(s -> s)//
                     .distinct()//
                     .map(name -> new PropertyAccessor(fields.get(name), getters.get(name), setters.get(name), config))//
                     .collect(Collectors.toMap(pa -> pa.name, pa -> pa));
    }

    private static String determineName(Field field, Method getter, Method setter, Config config) {
        if (getter != null) {
            return U.getPropertyName(getter, config);
        }
        if (setter != null) {
            return U.getPropertyName(setter, config);
        }
        if (field != null) {
            return U.getPropertyName(field, config);
        }
        throw new RuntimeException("unreachable");
    }

    private final String                 name;
    private final Field                  field;
    private final Method                 getter;
    private final Method                 setter;
    private       CoercingPropertySetter cpsCache = null;

    private PropertyAccessor(String name, Field field, Method getter, Method setter) {
        this.name   = name;
        this.field  = field;
        this.getter = getter;
        this.setter = setter;
    }

    private PropertyAccessor(Field field, Method getter, Method setter, Config config) {
        this(determineName(field, getter, setter, config), field, getter, setter);
    }

    public String name() {
        return name;
    }

    boolean canGet() {
        return field != null || getter != null;
    }

    boolean canSet() {
        return field != null || setter != null;
    }

    boolean isId(Config config) {
        return field != null && config.getAnnotation(field, JsonId.class) != null //
               || getter != null && config.getAnnotation(getter, JsonId.class) != null //
               || setter != null && config.getAnnotation(setter, JsonId.class) != null;

    }

    Type type() {
        if (getter != null) {
            return getter.getGenericReturnType();
        }
        if (setter != null) {
            return setter.getGenericParameterTypes()[0];
        }
        if (field != null) {
            return field.getGenericType();
        }
        throw new RuntimeException("unreachable");
    }

    Class<?> clazz() {
        return U.getRawClassOf(type());
    }

    CoercingPropertySetter getCoercingPropertySetter(Config config) {
        if (cpsCache == null) {
            cpsCache = CoercingPropertySetter.forProperty(this, config);
        }
        return cpsCache;
    }

    @SuppressWarnings("unchecked")
    <T> T get(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("can not get property '" + name + "' of <null>");
        }
        if (!canGet()) {
            throw new IllegalArgumentException("no getter or field can be determined for property '" + name + "' of " + obj.getClass().getName());
        }
        try {
            return getter != null ? (T) getter.invoke(obj) : (T) field.get(obj);
        } catch (InvocationTargetException |
                 IllegalAccessException e) {
            throw new RuntimeException("problem encountered getting property '" + name + "' of " + obj.getClass().getName(), e);
        }
    }

    void set(Object obj, Object value) {
        if (obj == null) {
            throw new IllegalArgumentException("can not set property '" + name + "' on <null>");
        }
        if (!canSet()) {
            throw new IllegalArgumentException("no setter or field can be determined for property '" + name + "' of " + obj.getClass().getName());
        }
        try {
            if (setter != null) {
                setter.invoke(obj, value);
            } else {
                field.set(obj, value);
            }
        } catch (InvocationTargetException |
                 IllegalAccessException e) {
            throw new RuntimeException("problem encountered getting property '" + name + "' of " + obj.getClass().getName(), e);
        }
    }
}
