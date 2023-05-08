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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

interface SubSetter {
    // if (o==Collection) o.add(v) or o[key]=v (key=Integer)
    // if (o==Map       ) o.put(key,v)
    // if (o==Object    ) o.key = v
    Object set(Object o, Object key, Object v);

    @SuppressWarnings({"unchecked", "Convert2MethodRef"})
    class CoercingMemberSetter implements SubSetter {
        @SuppressWarnings({"FieldCanBeLocal", "unused"})
        private final String    name;
        private final SubSetter nullFieldSetter;
        private final SubSetter longFieldSetter;
        private final SubSetter doubleFieldSetter;
        private final SubSetter boolFieldSetter;
        private final SubSetter stringFieldSetter;
        private final SubSetter objectFieldSetter;

        public static SubSetter forCollection(Type subType) {
            Class<?> clazz = U.getRawClassOf(subType);

            if (clazz == long.class || clazz == Long.class) {
                return new CoercingMemberSetter("long array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Long) null),
                        (o, key, v) -> setCollectionElement(o, (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, Long.parseLong((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == double.class || clazz == Double.class) {
                return new CoercingMemberSetter("double array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Double) null),
                        (o, key, v) -> setCollectionElement(o, (double) (long) v),
                        (o, key, v) -> setCollectionElement(o, (double) v),
                        null,
                        (o, key, v) -> setCollectionElement(o, Double.parseDouble((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == int.class || clazz == Integer.class) {
                return new CoercingMemberSetter("int array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Integer) null),
                        (o, key, v) -> setCollectionElement(o, (int) (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, Integer.parseInt((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == boolean.class || clazz == Boolean.class) {
                return new CoercingMemberSetter("boolean array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Boolean) null),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, (boolean) v),
                        (o, key, v) -> setCollectionElement(o, Boolean.parseBoolean((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == byte.class || clazz == Byte.class) {
                return new CoercingMemberSetter("byte array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Byte) null),
                        (o, key, v) -> setCollectionElement(o, (byte) (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, Byte.parseByte((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == short.class || clazz == Short.class) {
                return new CoercingMemberSetter("short array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Short) null),
                        (o, key, v) -> setCollectionElement(o, (short) (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, Short.parseShort((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == char.class || clazz == Character.class) {
                return new CoercingMemberSetter("char array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Character) null),
                        (o, key, v) -> setCollectionElement(o, (char) (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, ((String) v).charAt(0)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == float.class || clazz == Float.class) {
                return new CoercingMemberSetter("float array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Float) null),
                        (o, key, v) -> setCollectionElement(o, (float) (long) v),
                        (o, key, v) -> setCollectionElement(o, (float) (double) v),
                        null,
                        (o, key, v) -> setCollectionElement(o, Float.parseFloat((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == String.class) {
                return new CoercingMemberSetter("String array",
                        (o, key, v) -> setCollectionElement(o, null),
                        (o, key, v) -> setCollectionElement(o, Long.toString((long) v)),
                        (o, key, v) -> setCollectionElement(o, Double.toString((double) v)),
                        (o, key, v) -> setCollectionElement(o, Boolean.toString((boolean) v)),
                        (o, key, v) -> setCollectionElement(o, (String) v),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == Iterable.class) {
                return new CoercingMemberSetter("Iterable array",
                        (o, key, v) -> setCollectionElement(o, (Iterable<?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == Map.class) {
                return new CoercingMemberSetter("Map array",
                        (o, key, v) -> setCollectionElement(o, (Map<?, ?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            return new CoercingMemberSetter("other array",
                    (o, key, v) -> setCollectionElement(o, (Object) null),
                    null,
                    null,
                    null,
                    null,
                    (o, key, v) -> setCollectionElement(o, v)
            );
        }

        public static SubSetter forMap(Type subType) {
            Class<?> clazz = U.getRawClassOf(subType);

            if (clazz == long.class || clazz == Long.class) {
                return new CoercingMemberSetter("long Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Long) null),
                        (o, key, v) -> setMapElement(o, key, (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, Long.parseLong((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == double.class || clazz == Double.class) {
                return new CoercingMemberSetter("double Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Double) null),
                        (o, key, v) -> setMapElement(o, key, (double) (long) v),
                        (o, key, v) -> setMapElement(o, key, (double) v),
                        null,
                        (o, key, v) -> setMapElement(o, key, Double.parseDouble((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == int.class || clazz == Integer.class) {
                return new CoercingMemberSetter("int Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Integer) null),
                        (o, key, v) -> setMapElement(o, key, (int) (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, Integer.parseInt((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == boolean.class || clazz == Boolean.class) {
                return new CoercingMemberSetter("boolean Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Boolean) null),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, (boolean) v),
                        (o, key, v) -> setMapElement(o, key, Boolean.parseBoolean((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == byte.class || clazz == Byte.class) {
                return new CoercingMemberSetter("long Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Byte) null),
                        (o, key, v) -> setMapElement(o, key, (byte) (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, Byte.parseByte((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == short.class || clazz == Short.class) {
                return new CoercingMemberSetter("short Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Short) null),
                        (o, key, v) -> setMapElement(o, key, (short) (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, Short.parseShort((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == char.class || clazz == Character.class) {
                return new CoercingMemberSetter("char Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Character) null),
                        (o, key, v) -> setMapElement(o, key, (char) (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, ((String) v).charAt(0)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == float.class || clazz == Float.class) {
                return new CoercingMemberSetter("float Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Float) null),
                        (o, key, v) -> setMapElement(o, key, (float) (long) v),
                        (o, key, v) -> setMapElement(o, key, (float) (double) v),
                        null,
                        (o, key, v) -> setMapElement(o, key, Float.parseFloat((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == String.class) {
                return new CoercingMemberSetter("String Map",
                        (o, key, v) -> setMapElement(o, key, null),
                        (o, key, v) -> setMapElement(o, key, Long.toString((long) v)),
                        (o, key, v) -> setMapElement(o, key, Double.toString((double) v)),
                        (o, key, v) -> setMapElement(o, key, Boolean.toString((boolean) v)),
                        (o, key, v) -> setMapElement(o, key, (String) v),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == Iterable.class) {
                return new CoercingMemberSetter("Iterable Map",
                        (o, key, v) -> setMapElement(o, key, (Iterable<?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == Map.class) {
                return new CoercingMemberSetter("Map Map",
                        (o, key, v) -> setMapElement(o, key, (Map<?, ?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            return new CoercingMemberSetter("other Map",
                    (o, key, v) -> setMapElement(o, key, (Object) null),
                    (o, key, v) -> setMapElement(o, key, v),
                    (o, key, v) -> setMapElement(o, key, v),
                    (o, key, v) -> setMapElement(o, key, v),
                    (o, key, v) -> setMapElement(o, key, v),
                    (o, key, v) -> setMapElement(o, key, v)
            );
        }

        public static CoercingMemberSetter forField(Field f, boolean ignoreSFO) {
            Class<?> clazz = f.getType();
            if (clazz == long.class || clazz == Long.class) {
                return new CoercingMemberSetter("long field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Long) null),
                        (o, key, v) -> setField(f, o, (long) v),
                        (o, key, v) -> setField(f, o, (long) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Long.parseLong((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == double.class || clazz == Double.class) {
                return new CoercingMemberSetter("double field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Double) null),
                        (o, key, v) -> setField(f, o, (double) (long) v),
                        (o, key, v) -> setField(f, o, (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Double.parseDouble((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == int.class || clazz == Integer.class) {
                return new CoercingMemberSetter("int field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Integer) null),
                        (o, key, v) -> setField(f, o, (int) (long) v),
                        (o, key, v) -> setField(f, o, (int) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Integer.parseInt((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == boolean.class || clazz == Boolean.class) {
                return new CoercingMemberSetter("boolean field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Boolean) null),
                        null,
                        null,
                        (o, key, v) -> setField(f, o, (boolean) v),
                        (o, key, v) -> setField(f, o, Boolean.parseBoolean((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == byte.class || clazz == Byte.class) {
                return new CoercingMemberSetter("byte field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Byte) null),
                        (o, key, v) -> setField(f, o, (byte) (long) v),
                        (o, key, v) -> setField(f, o, (byte) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Byte.parseByte((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == short.class || clazz == Short.class) {
                return new CoercingMemberSetter("short field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Short) null),
                        (o, key, v) -> setField(f, o, (short) (long) v),
                        (o, key, v) -> setField(f, o, (short) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Short.parseShort((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == char.class || clazz == Character.class) {
                return new CoercingMemberSetter("char field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Character) null),
                        (o, key, v) -> setField(f, o, (char) (long) v),
                        null,
                        null,
                        (o, key, v) -> setField(f, o, ((String) v).charAt(0)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == float.class || clazz == Float.class) {
                return new CoercingMemberSetter("float field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Float) null),
                        (o, key, v) -> setField(f, o, (float) (long) v),
                        (o, key, v) -> setField(f, o, (float) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Float.parseFloat((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == String.class) {
                return new CoercingMemberSetter("String field " + f.getName(),
                        (o, key, v) -> setField(f, o, null),
                        (o, key, v) -> setField(f, o, Long.toString((long) v)),
                        (o, key, v) -> setField(f, o, Double.toString((double) v)),
                        (o, key, v) -> setField(f, o, Boolean.toString((boolean) v)),
                        (o, key, v) -> setField(f, o, (String) v),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == Iterable.class) {
                return new CoercingMemberSetter("Iterable field " + f.getName(),
                        (o, key, v) -> setField(f, o, (Iterable<?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == Map.class) {
                return new CoercingMemberSetter("Map field " + f.getName(),
                        (o, key, v) -> setField(f, o, (Map<?, ?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setField(f, o, v)
                );
            }
            SubSetter longFieldSetter    = null;
            SubSetter doubleFieldSetter  = null;
            SubSetter booleanFieldSetter = null;
            SubSetter stringFieldSetter  = null;
            if (!ignoreSFO) {
                longFieldSetter    = makeConstructorSubSetter(null, f, long.class, x -> (long) x);
                longFieldSetter    = makeConstructorSubSetter(longFieldSetter, f, int.class, x -> (int) (long) x);
                longFieldSetter    = makeConstructorSubSetter(longFieldSetter, f, short.class, x -> (short) (long) x);
                longFieldSetter    = makeConstructorSubSetter(longFieldSetter, f, byte.class, x -> (byte) (long) x);
                longFieldSetter    = makeConstructorSubSetter(longFieldSetter, f, char.class, x -> (char) (long) x);
                doubleFieldSetter  = makeConstructorSubSetter(null, f, double.class, x -> (double) x);
                doubleFieldSetter  = makeConstructorSubSetter(doubleFieldSetter, f, float.class, x -> (float) (double) x);
                booleanFieldSetter = makeConstructorSubSetter(null, f, boolean.class, x -> (boolean) x);
                stringFieldSetter  = makeConstructorSubSetter(null, f, String.class, x -> (String) x);
            }

            return new CoercingMemberSetter("other field " + f.getName(),
                    (o, key, v) -> setField(f, o, (Object) null),
                    longFieldSetter,
                    doubleFieldSetter,
                    booleanFieldSetter,
                    stringFieldSetter,
                    (o, key, v) -> setField(f, o, v)
            );
        }

        public static CoercingMemberSetter forMethod(Method m, boolean ignoreSFO) {
            Class<?> clazz = m.getParameterTypes()[0];
            if (clazz == long.class || clazz == Long.class) {
                return new CoercingMemberSetter("long method " + m.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setMethod(m, o, (Long) null),
                        (o, key, v) -> setMethod(m, o, (long) v),
                        (o, key, v) -> setMethod(m, o, (long) (double) v),
                        null,
                        (o, key, v) -> setMethod(m, o, Long.parseLong((String) v)),
                        (o, key, v) -> setMethod(m, o, v)
                );
            }
            if (clazz == double.class || clazz == Double.class) {
                return new CoercingMemberSetter("double method " + m.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setMethod(m, o, (Double) null),
                        (o, key, v) -> setMethod(m, o, (double) (long) v),
                        (o, key, v) -> setMethod(m, o, (double) v),
                        null,
                        (o, key, v) -> setMethod(m, o, Double.parseDouble((String) v)),
                        (o, key, v) -> setMethod(m, o, v)
                );
            }
            if (clazz == int.class || clazz == Integer.class) {
                return new CoercingMemberSetter("int method " + m.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setMethod(m, o, (Integer) null),
                        (o, key, v) -> setMethod(m, o, (int) (long) v),
                        (o, key, v) -> setMethod(m, o, (int) (double) v),
                        null,
                        (o, key, v) -> setMethod(m, o, Integer.parseInt((String) v)),
                        (o, key, v) -> setMethod(m, o, v)
                );
            }
            if (clazz == boolean.class || clazz == Boolean.class) {
                return new CoercingMemberSetter("boolean method " + m.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setMethod(m, o, (Boolean) null),
                        null,
                        null,
                        (o, key, v) -> setMethod(m, o, (boolean) v),
                        (o, key, v) -> setMethod(m, o, Boolean.parseBoolean((String) v)),
                        (o, key, v) -> setMethod(m, o, v)
                );
            }
            if (clazz == byte.class || clazz == Byte.class) {
                return new CoercingMemberSetter("byte method " + m.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setMethod(m, o, (Byte) null),
                        (o, key, v) -> setMethod(m, o, (byte) (long) v),
                        (o, key, v) -> setMethod(m, o, (byte) (double) v),
                        null,
                        (o, key, v) -> setMethod(m, o, Byte.parseByte((String) v)),
                        (o, key, v) -> setMethod(m, o, v)
                );
            }
            if (clazz == short.class || clazz == Short.class) {
                return new CoercingMemberSetter("short method " + m.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setMethod(m, o, (Short) null),
                        (o, key, v) -> setMethod(m, o, (short) (long) v),
                        (o, key, v) -> setMethod(m, o, (short) (double) v),
                        null,
                        (o, key, v) -> setMethod(m, o, Short.parseShort((String) v)),
                        (o, key, v) -> setMethod(m, o, v)
                );
            }
            if (clazz == char.class || clazz == Character.class) {
                return new CoercingMemberSetter("char method " + m.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setMethod(m, o, (Character) null),
                        (o, key, v) -> setMethod(m, o, (char) (long) v),
                        null,
                        null,
                        (o, key, v) -> setMethod(m, o, ((String) v).charAt(0)),
                        (o, key, v) -> setMethod(m, o, v)
                );
            }
            if (clazz == float.class || clazz == Float.class) {
                return new CoercingMemberSetter("float method " + m.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setMethod(m, o, (Float) null),
                        (o, key, v) -> setMethod(m, o, (float) (long) v),
                        (o, key, v) -> setMethod(m, o, (float) (double) v),
                        null,
                        (o, key, v) -> setMethod(m, o, Float.parseFloat((String) v)),
                        (o, key, v) -> setMethod(m, o, v)
                );
            }
            if (clazz == String.class) {
                return new CoercingMemberSetter("String method " + m.getName(),
                        (o, key, v) -> setMethod(m, o, null),
                        (o, key, v) -> setMethod(m, o, Long.toString((long) v)),
                        (o, key, v) -> setMethod(m, o, Double.toString((double) v)),
                        (o, key, v) -> setMethod(m, o, Boolean.toString((boolean) v)),
                        (o, key, v) -> setMethod(m, o, (String) v),
                        (o, key, v) -> setMethod(m, o, v)
                );
            }
            if (clazz == Iterable.class) {
                return new CoercingMemberSetter("Iterable method " + m.getName(),
                        (o, key, v) -> setMethod(m, o, (Iterable<?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setMethod(m, o, v)
                );
            }
            if (clazz == Map.class) {
                return new CoercingMemberSetter("Map method " + m.getName(),
                        (o, key, v) -> setMethod(m, o, (Map<?, ?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setMethod(m, o, v)
                );
            }
            SubSetter longFieldSetter    = null;
            SubSetter doubleFieldSetter  = null;
            SubSetter booleanFieldSetter = null;
            SubSetter stringFieldSetter  = null;
            if (!ignoreSFO) {
                longFieldSetter    = makeConstructorSubSetter(null, m, long.class, x -> (long) x);
                longFieldSetter    = makeConstructorSubSetter(longFieldSetter, m, int.class, x -> (int) (long) x);
                longFieldSetter    = makeConstructorSubSetter(longFieldSetter, m, short.class, x -> (short) (long) x);
                longFieldSetter    = makeConstructorSubSetter(longFieldSetter, m, byte.class, x -> (byte) (long) x);
                longFieldSetter    = makeConstructorSubSetter(longFieldSetter, m, char.class, x -> (char) (long) x);
                doubleFieldSetter  = makeConstructorSubSetter(null, m, double.class, x -> (double) x);
                doubleFieldSetter  = makeConstructorSubSetter(doubleFieldSetter, m, float.class, x -> (float) (double) x);
                booleanFieldSetter = makeConstructorSubSetter(null, m, boolean.class, x -> (boolean) x);
                stringFieldSetter  = makeConstructorSubSetter(null, m, String.class, x -> (String) x);
            }

            return new CoercingMemberSetter("other method " + m.getName(),
                    (o, key, v) -> setMethod(m, o, (Object) null),
                    longFieldSetter,
                    doubleFieldSetter,
                    booleanFieldSetter,
                    stringFieldSetter,
                    (o, key, v) -> setMethod(m, o, v)
            );
        }

        private static <PRIM> SubSetter makeConstructorSubSetter(SubSetter setter, Field f, Class<PRIM> primClass, Function<Object, PRIM> convert) {
            return setter != null ? setter : Arrays.stream(f.getType().getConstructors())
                    .filter(c -> c.getParameterCount() == 1 && (c.getParameterTypes()[0] == primClass || c.getParameterTypes()[0] == U.box(primClass)))
                    .findFirst()
                    .map(c -> (SubSetter) (o, key, v) -> {
                        try {
                            setField(f, o, c.newInstance(convert.apply(v)));
                            return o;
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            throw new IllegalArgumentException("can not make new instance of " + c.getDeclaringClass().getSimpleName() + " with arg (" + v + ")", e);
                        }
                    })
                    .orElse(null);
        }

        private static <PRIM> SubSetter makeConstructorSubSetter(SubSetter setter, Method m, Class<PRIM> primClass, Function<Object, PRIM> convert) {
            return setter != null ? setter : Arrays.stream(m.getParameterTypes()[0].getConstructors())
                    .filter(c -> c.getParameterCount() == 1 && (c.getParameterTypes()[0] == primClass || c.getParameterTypes()[0] == U.box(primClass)))
                    .findFirst()
                    .map(c -> (SubSetter) (o, key, v) -> {
                        try {
                            setMethod(m, o, c.newInstance(convert.apply(v)));
                            return o;
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            throw new IllegalArgumentException("can not make new instance of " + c.getDeclaringClass().getSimpleName() + " with arg (" + v + ")", e);
                        }
                    })
                    .orElse(null);
        }

        public CoercingMemberSetter(String name, SubSetter nullFieldSetter, SubSetter longFieldSetter, SubSetter doubleFieldSetter, SubSetter boolFieldSetter, SubSetter stringFieldSetter, SubSetter objectFieldSetter) {
            this.name              = name;
            this.nullFieldSetter   = nullFieldSetter;
            this.longFieldSetter   = longFieldSetter;
            this.doubleFieldSetter = doubleFieldSetter;
            this.boolFieldSetter   = boolFieldSetter;
            this.stringFieldSetter = stringFieldSetter;
            this.objectFieldSetter = objectFieldSetter;
        }

        @Override
        public Object set(Object o, Object key, Object v) {
            try {
                Class<?> valueClass = v == null ? null : v.getClass();
                if (valueClass == null) {
                    notNull(nullFieldSetter, o, key, v).set(o, key, v);
                } else if (valueClass == Long.class) {
                    notNull(longFieldSetter, o, key, v).set(o, key, v);
                } else if (valueClass == Double.class) {
                    notNull(doubleFieldSetter, o, key, v).set(o, key, v);
                } else if (valueClass == Boolean.class) {
                    notNull(boolFieldSetter, o, key, v).set(o, key, v);
                } else if (valueClass == String.class) {
                    notNull(stringFieldSetter, o, key, v).set(o, key, v);
                } else {
                    notNull(objectFieldSetter, o, key, v).set(o, key, v);
                }
                return o;
            } catch (Exception e) {
                throw new IllegalArgumentException("can't set " + o.getClass().getSimpleName() + "[" + key + "] = " + v, e);
            }
        }

        private static SubSetter notNull(SubSetter ss, Object o, Object key, Object v) {
            if (ss == null) {
                throw new IllegalArgumentException("field " + key + "of a " + o.getClass().getSimpleName() + " can not be set to " + (v == null ? "null" : "a " + v.getClass().getSimpleName()));
            }
            return ss;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        private static Object setField(Field f, Object o, Object v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setField(Field f, Object o, long v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setField(Field f, Object o, int v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setField(Field f, Object o, double v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setField(Field f, Object o, String v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setField(Field f, Object o, boolean v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setField(Field f, Object o, short v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setField(Field f, Object o, byte v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setField(Field f, Object o, char v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setField(Field f, Object o, float v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        private static Object setMethod(Method m, Object o, Object v) {
            try {
                m.invoke(o, v);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setMethod(Method m, Object o, long v) {
            try {
                m.invoke(o, v);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setMethod(Method m, Object o, int v) {
            try {
                m.invoke(o, v);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setMethod(Method m, Object o, double v) {
            try {
                m.invoke(o, v);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setMethod(Method m, Object o, String v) {
            try {
                m.invoke(o, v);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setMethod(Method m, Object o, boolean v) {
            try {
                m.invoke(o, v);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setMethod(Method m, Object o, short v) {
            try {
                m.invoke(o, v);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setMethod(Method m, Object o, byte v) {
            try {
                m.invoke(o, v);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setMethod(Method m, Object o, char v) {
            try {
                m.invoke(o, v);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        private static Object setMethod(Method m, Object o, float v) {
            try {
                m.invoke(o, v);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        private static Object setCollectionElement(Object o, Object v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Object>) o).add(v);
            return o;
        }

        private static Object setCollectionElement(Object o, long v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Long>) o).add(v);
            return o;
        }

        private static Object setCollectionElement(Object o, int v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Integer>) o).add(v);
            return o;
        }

        private static Object setCollectionElement(Object o, double v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Double>) o).add(v);
            return o;
        }

        private static Object setCollectionElement(Object o, String v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<String>) o).add(v);
            return o;
        }

        private static Object setCollectionElement(Object o, boolean v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Boolean>) o).add(v);
            return o;
        }

        private static Object setCollectionElement(Object o, short v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Short>) o).add(v);
            return o;
        }

        private static Object setCollectionElement(Object o, byte v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Byte>) o).add(v);
            return o;
        }

        private static Object setCollectionElement(Object o, char v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Character>) o).add(v);
            return o;
        }

        private static Object setCollectionElement(Object o, float v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Float>) o).add(v);
            return o;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        private static Object setMapElement(Object o, Object key, Object v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Object>) o).put(key, v);
            return o;
        }

        private static Object setMapElement(Object o, Object key, long v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Long>) o).put(key, v);
            return o;
        }

        private static Object setMapElement(Object o, Object key, int v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Integer>) o).put(key, v);
            return o;
        }

        private static Object setMapElement(Object o, Object key, double v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Double>) o).put(key, v);
            return o;
        }

        private static Object setMapElement(Object o, Object key, String v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, String>) o).put(key, v);
            return o;
        }

        private static Object setMapElement(Object o, Object key, boolean v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Boolean>) o).put(key, v);
            return o;
        }

        private static Object setMapElement(Object o, Object key, short v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Short>) o).put(key, v);
            return o;
        }

        private static Object setMapElement(Object o, Object key, byte v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Byte>) o).put(key, v);
            return o;
        }

        private static Object setMapElement(Object o, Object key, char v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Character>) o).put(key, v);
            return o;
        }

        private static Object setMapElement(Object o, Object key, float v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Float>) o).put(key, v);
            return o;
        }
    }
}
