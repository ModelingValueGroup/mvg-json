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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class U {
    public static final  String            CLASS_NAME_FIELD_NAME           = "~className";
    private static final Predicate<Field>  FIELD_INTROSPECTION_FILTER      = f -> !f.isSynthetic()//
                                                                                  && !f.isEnumConstant()//
                                                                                  && !Modifier.isStatic(f.getModifiers())//
                                                                                  && !Modifier.isVolatile(f.getModifiers())//
                                                                                  && !Modifier.isNative(f.getModifiers())//
                                                                                  && !Modifier.isTransient(f.getModifiers())//
                                                                                  && f.getAnnotation(JsonIgnore.class) == null
                                                                                  && (Modifier.isPublic(f.getModifiers()) || !f.getDeclaringClass().getPackage().getName().startsWith("java."));
    private static final Predicate<Method> GET_METHOD_INTROSPECTION_FILTER = m -> !m.isSynthetic()//
                                                                                  && m.getParameterCount() == 0//
                                                                                  && m.getReturnType() != Void.class//
                                                                                  && !m.isDefault()//
                                                                                  && !Modifier.isStatic(m.getModifiers())//
                                                                                  && !Modifier.isVolatile(m.getModifiers())//
                                                                                  && !Modifier.isNative(m.getModifiers())//
                                                                                  && !Modifier.isTransient(m.getModifiers())//
                                                                                  && m.getAnnotation(JsonIgnore.class) == null
                                                                                  && (Modifier.isPublic(m.getModifiers()) || !m.getDeclaringClass().getPackage().getName().startsWith("java."))//
                                                                                  && m.getName().matches("^(get|is)[A-Z].*");

    private static final Predicate<Method> SET_METHOD_INTROSPECTION_FILTER = m -> !m.isSynthetic()//
                                                                                  && m.getParameterCount() == 1//
                                                                                  && !m.isDefault()//
                                                                                  && !Modifier.isStatic(m.getModifiers())//
                                                                                  && !Modifier.isVolatile(m.getModifiers())//
                                                                                  && !Modifier.isNative(m.getModifiers())//
                                                                                  && !Modifier.isTransient(m.getModifiers())//
                                                                                  && m.getAnnotation(JsonIgnore.class) == null
                                                                                  && (Modifier.isPublic(m.getModifiers()) || !m.getDeclaringClass().getPackage().getName().startsWith("java."))//
                                                                                  && m.getName().matches("^set[A-Z].*");

    public enum RW {
        READ, WRITE
    }

    public static String fieldToElementName(Field f, Config config) {
        JsonName nameAnno = config.getAnnotation(f, JsonName.class);
        return nameAnno == null ? f.getName() : nameAnno.value();
    }

    public static String methodToElementName(Method m) {
        return m.getName().replaceAll("^(set|get|is)([A-Z]).*", "$2").toLowerCase() + m.getName().replaceAll("^(set|get|is)[A-Z]", "");
    }

    public static Class<?> getRawClassOf(Type type) {
        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            if (rawType instanceof Class) {
                return (Class<?>) rawType;
            }
        } else if (type instanceof Class) {
            return (Class<?>) type;
        }
        throw new IllegalArgumentException("no raw class can be determined for " + type);
    }

    public static Type getElementType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            List<Type>        args  = Arrays.stream(ptype.getActualTypeArguments()).collect(Collectors.toList());
            if (args.size() == 0) {
                return null;
            }
            if (args.size() == 1) {
                return args.get(0);
            }
            if (args.size() == 2) {
                return args.get(1);
            }
        } else if (type instanceof Class) {
            return null;
        }
        throw new IllegalArgumentException("no element type can be determined for " + type);
    }

    public static Class<?> box(Class<?> t) {
        if (t.isPrimitive()) {
            if (t == long.class) {
                return Long.class;
            }
            if (t == int.class) {
                return Integer.class;
            }
            if (t == short.class) {
                return Short.class;
            }
            if (t == byte.class) {
                return Byte.class;
            }
            if (t == char.class) {
                return Character.class;
            }
            if (t == double.class) {
                return Double.class;
            }
            if (t == float.class) {
                return Float.class;
            }
            if (t == boolean.class) {
                return Boolean.class;
            }
        }
        return t;
    }

    public static Class<?> unbox(Class<?> t) {
        if (t == Long.class) {
            return long.class;
        }
        if (t == Integer.class) {
            return int.class;
        }
        if (t == Short.class) {
            return short.class;
        }
        if (t == Byte.class) {
            return byte.class;
        }
        if (t == Character.class) {
            return char.class;
        }
        if (t == Double.class) {
            return double.class;
        }
        if (t == Float.class) {
            return float.class;
        }
        if (t == Boolean.class) {
            return boolean.class;
        }
        return t;
    }

    static void findElements(Class<?> clazz, Config config, Consumer<Method> methodConsumer, Consumer<Field> fieldConsumer, RW rw) {
        Set<String> names = new HashSet<>();
        for (Class<?> c = clazz; c != Object.class; c = c.getSuperclass()) {
            Arrays.stream(c.getDeclaredMethods())
                    .filter(rw == RW.READ ? GET_METHOD_INTROSPECTION_FILTER : SET_METHOD_INTROSPECTION_FILTER)
                    .peek(m -> m.setAccessible(true))
                    .filter(m -> names.add(methodToElementName(m)))
                    .forEach(methodConsumer);
            Arrays.stream(c.getDeclaredFields())
                    .filter(FIELD_INTROSPECTION_FILTER)
                    .peek(f -> f.setAccessible(true))
                    .filter(f -> names.add(fieldToElementName(f, config)))
                    .forEach(fieldConsumer);
        }
    }
}
