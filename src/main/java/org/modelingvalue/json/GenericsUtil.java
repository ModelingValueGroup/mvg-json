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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GenericsUtil {
    interface Constructor {
        Object construct();
    }

    interface SubSetter {
        // if (o==Collection) o.add(v) or o[key]=v (key=Integer)
        // if (o==Map       ) o.put(key,v)
        // if (o==Object    ) o.key = v
        void set(Object o, Object key, Object v);
    }

    interface TypeInfo {
        Constructor getConstructor();

        Type getSubType(Object key);

        SubSetter getSubSetter(Object key);
    }


    public abstract static class BaseTypeInfo implements TypeInfo {
        public final Class<?>    clazz;
        public final Constructor constructor;

        public BaseTypeInfo(Class<?> clazz) {
            this.clazz       = clazz;
            this.constructor = makeConstructor(clazz);
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public Constructor getConstructor() {
            return constructor;
        }
    }

    public static class ObjectTypeInfo extends BaseTypeInfo {
        public static class FieldInfo {
            public final Type      type;
            public final SubSetter subSetter;

            private FieldInfo(Field f) {
                this.type      = f.getGenericType();
                this.subSetter = CoercingFieldSetter.forField(f);
            }
        }

        public final Map<String, FieldInfo> fieldInfoMap;

        public ObjectTypeInfo(Class<?> clazz) {
            super(clazz);
            this.fieldInfoMap = getAllFields(clazz).stream()
                    .peek(f -> f.setAccessible(true))
                    .filter(f -> !f.isSynthetic() && !Modifier.isFinal(f.getModifiers()) && !Modifier.isStatic(f.getModifiers()))
                    .collect(Collectors.toMap(GenericsUtil::getFieldTag, FieldInfo::new));
        }

        @Override
        public Type getSubType(Object key) {
            return getFieldInfo(key).type;
        }

        public SubSetter getSubSetter(Object key) {
            return getFieldInfo(key).subSetter;
        }

        private FieldInfo getFieldInfo(Object key) {
            if (!(key instanceof String)) {
                throw new Error("unexpected key type (String expected): " + key.getClass().getSimpleName());
            }
            FieldInfo fieldInfo = fieldInfoMap.get((String) key);
            if (fieldInfo == null) {
                throw new Error("the type " + clazz.getSimpleName() + " does not have a field " + key);
            }
            return fieldInfo;
        }

        private static List<Field> getAllFields(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                fields.addAll(Arrays.asList(c.getDeclaredFields()));
            }
            return fields;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[class " + getClazz().getSimpleName() + " with " + fieldInfoMap.size() + " fields]";
        }
    }

    public static class CollectionTypeInfo extends BaseTypeInfo {
        private final Type      subType;
        public final  SubSetter subSetter;

        public CollectionTypeInfo(Class<?> clazz, Type elementType) {
            super(clazz);
            this.subType   = elementType;
            this.subSetter = CoercingFieldSetter.forCollection(elementType);
        }

        @Override
        public Type getSubType(Object key) {
            return subType;
        }

        @Override
        public SubSetter getSubSetter(Object key) {
            return subSetter;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + getClazz().getSimpleName() + "]";
        }
    }

    public static class MapTypeInfo extends BaseTypeInfo {
        private final Type      elementType;
        public final  SubSetter subSetter;

        public MapTypeInfo(Class<?> clazz, Type elementType) {
            super(clazz);
            this.elementType = elementType;
            this.subSetter   = CoercingFieldSetter.forMap(elementType);
        }

        @Override
        public Type getSubType(Object key) {
            return elementType;
        }

        @Override
        public Constructor getConstructor() {
            return super.getConstructor();
        }

        @Override
        public SubSetter getSubSetter(Object key) {
            return subSetter;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + getClazz().getSimpleName() + "]";
        }
    }

    public static TypeInfo makeTypeInfo(Type type) {
        Class<?> rawClass = getRawClassOf(type);

        if (Map.class.isAssignableFrom(rawClass)) {
            return new MapTypeInfo(rawClass, getElementType(type));
        } else if (Collection.class.isAssignableFrom(rawClass)) {
            return new CollectionTypeInfo(rawClass, getElementType(type));
        } else if (Object.class.isAssignableFrom(rawClass)) {
            return new ObjectTypeInfo(rawClass);
        }
        throw new IllegalArgumentException("no TypeInfoMap for " + rawClass.getSimpleName() + " possible");
    }

    private static String getFieldTag(Field f) {
        JsonName nameAnno = f.getAnnotation(JsonName.class);
        return nameAnno == null ? f.getName() : nameAnno.value();
    }

    private static Constructor makeConstructor(Class<?> t) {
        if (t.isInterface()) {
            if (t.isAssignableFrom(List.class)) {
                return ArrayList::new;
            }
            if (t.isAssignableFrom(Set.class)) {
                return HashSet::new;
            }
            if (t.isAssignableFrom(Map.class)) {
                return HashMap::new;
            }
            throw new Error("problem instanciating interface " + t.getSimpleName());
        }
        return () -> {
            try {
                return t.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new Error("problem instanciating " + t.getSimpleName(), e);
            }
        };
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

    @SuppressWarnings({"unchecked", "Convert2MethodRef"})
    private static class CoercingFieldSetter implements SubSetter {
        private final SubSetter nullFieldSetter;
        private final SubSetter longFieldSetter;
        private final SubSetter doubleFieldSetter;
        private final SubSetter boolFieldSetter;
        private final SubSetter stringFieldSetter;
        private final SubSetter objectFieldSetter;

        public static SubSetter forCollection(Type subType) {
            Class<?> clazz = getRawClassOf(subType);

            if (clazz == long.class || clazz == Long.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Long) null),
                        (o, key, v) -> setCollectionElement(o, (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, Long.parseLong((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == double.class || clazz == Double.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Double) null),
                        (o, key, v) -> setCollectionElement(o, (double) (long) v),
                        (o, key, v) -> setCollectionElement(o, (double) v),
                        null,
                        (o, key, v) -> setCollectionElement(o, Double.parseDouble((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == int.class || clazz == Integer.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Integer) null),
                        (o, key, v) -> setCollectionElement(o, (int) (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, Integer.parseInt((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == boolean.class || clazz == Boolean.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Boolean) null),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, (boolean) v),
                        (o, key, v) -> setCollectionElement(o, Boolean.parseBoolean((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == byte.class || clazz == Byte.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Byte) null),
                        (o, key, v) -> setCollectionElement(o, (byte) (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, Byte.parseByte((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == short.class || clazz == Short.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Short) null),
                        (o, key, v) -> setCollectionElement(o, (short) (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, Short.parseShort((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == char.class || clazz == Character.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Character) null),
                        (o, key, v) -> setCollectionElement(o, (char) (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, ((String) v).charAt(0)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == float.class || clazz == Float.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Float) null),
                        (o, key, v) -> setCollectionElement(o, (float) (long) v),
                        (o, key, v) -> setCollectionElement(o, (float) (double) v),
                        null,
                        (o, key, v) -> setCollectionElement(o, Float.parseFloat((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == String.class) {
                return new CoercingFieldSetter(
                        (o, key, v) -> setCollectionElement(o, null),
                        (o, key, v) -> setCollectionElement(o, Long.toString((long) v)),
                        (o, key, v) -> setCollectionElement(o, Double.toString((double) v)),
                        (o, key, v) -> setCollectionElement(o, Boolean.toString((boolean) v)),
                        (o, key, v) -> setCollectionElement(o, (String) v),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == Iterable.class) {
                return new CoercingFieldSetter(
                        (o, key, v) -> setCollectionElement(o, (Iterable<?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == Map.class) {
                return new CoercingFieldSetter(
                        (o, key, v) -> setCollectionElement(o, (Map<?, ?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            return new CoercingFieldSetter(
                    (o, key, v) -> setCollectionElement(o, (Object) null),
                    null,
                    null,
                    null,
                    null,
                    (o, key, v) -> setCollectionElement(o, v)
            );
        }

        public static SubSetter forMap(Type subType) {
            Class<?> clazz = getRawClassOf(subType);

            if (clazz == long.class || clazz == Long.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Long) null),
                        (o, key, v) -> setMapElement(o, key, (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, Long.parseLong((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == double.class || clazz == Double.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Double) null),
                        (o, key, v) -> setMapElement(o, key, (double) (long) v),
                        (o, key, v) -> setMapElement(o, key, (double) v),
                        null,
                        (o, key, v) -> setMapElement(o, key, Double.parseDouble((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == int.class || clazz == Integer.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Integer) null),
                        (o, key, v) -> setMapElement(o, key, (int) (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, Integer.parseInt((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == boolean.class || clazz == Boolean.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Boolean) null),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, (boolean) v),
                        (o, key, v) -> setMapElement(o, key, Boolean.parseBoolean((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == byte.class || clazz == Byte.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Byte) null),
                        (o, key, v) -> setMapElement(o, key, (byte) (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, Byte.parseByte((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == short.class || clazz == Short.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Short) null),
                        (o, key, v) -> setMapElement(o, key, (short) (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, Short.parseShort((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == char.class || clazz == Character.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Character) null),
                        (o, key, v) -> setMapElement(o, key, (char) (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, ((String) v).charAt(0)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == float.class || clazz == Float.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Float) null),
                        (o, key, v) -> setMapElement(o, key, (float) (long) v),
                        (o, key, v) -> setMapElement(o, key, (float) (double) v),
                        null,
                        (o, key, v) -> setMapElement(o, key, Float.parseFloat((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == String.class) {
                return new CoercingFieldSetter(
                        (o, key, v) -> setMapElement(o, key, null),
                        (o, key, v) -> setMapElement(o, key, Long.toString((long) v)),
                        (o, key, v) -> setMapElement(o, key, Double.toString((double) v)),
                        (o, key, v) -> setMapElement(o, key, Boolean.toString((boolean) v)),
                        (o, key, v) -> setMapElement(o, key, (String) v),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == Iterable.class) {
                return new CoercingFieldSetter(
                        (o, key, v) -> setMapElement(o, key, (Iterable<?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == Map.class) {
                return new CoercingFieldSetter(
                        (o, key, v) -> setMapElement(o, key, (Map<?, ?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            return new CoercingFieldSetter(
                    (o, key, v) -> setMapElement(o, key, (Object) null),
                    null,
                    null,
                    null,
                    null,
                    (o, key, v) -> setMapElement(o, key, v)
            );
        }

        private static CoercingFieldSetter forField(Field f) {
            Class<?> clazz = f.getType();
            if (clazz == long.class || clazz == Long.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Long) null),
                        (o, key, v) -> setField(f, o, (long) v),
                        (o, key, v) -> setField(f, o, (long) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Long.parseLong((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == double.class || clazz == Double.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Double) null),
                        (o, key, v) -> setField(f, o, (double) (long) v),
                        (o, key, v) -> setField(f, o, (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Double.parseDouble((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == int.class || clazz == Integer.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Integer) null),
                        (o, key, v) -> setField(f, o, (int) (long) v),
                        (o, key, v) -> setField(f, o, (int) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Integer.parseInt((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == boolean.class || clazz == Boolean.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Boolean) null),
                        null,
                        null,
                        (o, key, v) -> setField(f, o, (boolean) v),
                        (o, key, v) -> setField(f, o, Boolean.parseBoolean((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == byte.class || clazz == Byte.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Byte) null),
                        (o, key, v) -> setField(f, o, (byte) (long) v),
                        (o, key, v) -> setField(f, o, (byte) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Byte.parseByte((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == short.class || clazz == Short.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Short) null),
                        (o, key, v) -> setField(f, o, (short) (long) v),
                        (o, key, v) -> setField(f, o, (short) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Short.parseShort((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == char.class || clazz == Character.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Character) null),
                        (o, key, v) -> setField(f, o, (char) (long) v),
                        null,
                        null,
                        (o, key, v) -> setField(f, o, ((String) v).charAt(0)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == float.class || clazz == Float.class) {
                return new CoercingFieldSetter(
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Float) null),
                        (o, key, v) -> setField(f, o, (float) (long) v),
                        (o, key, v) -> setField(f, o, (float) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Float.parseFloat((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == String.class) {
                return new CoercingFieldSetter(
                        (o, key, v) -> setField(f, o, null),
                        (o, key, v) -> setField(f, o, Long.toString((long) v)),
                        (o, key, v) -> setField(f, o, Double.toString((double) v)),
                        (o, key, v) -> setField(f, o, Boolean.toString((boolean) v)),
                        (o, key, v) -> setField(f, o, (String) v),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == Iterable.class) {
                return new CoercingFieldSetter(
                        (o, key, v) -> setField(f, o, (Iterable<?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == Map.class) {
                return new CoercingFieldSetter(
                        (o, key, v) -> setField(f, o, (Map<?, ?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setField(f, o, v)
                );
            }
            SubSetter longFieldSetter = null;
            longFieldSetter = makeConstructorSubSetter(longFieldSetter, f, long.class, x -> (long) x);
            longFieldSetter = makeConstructorSubSetter(longFieldSetter, f, int.class, x -> (int) (long) x);
            longFieldSetter = makeConstructorSubSetter(longFieldSetter, f, short.class, x -> (short) (long) x);
            longFieldSetter = makeConstructorSubSetter(longFieldSetter, f, byte.class, x -> (byte) (long) x);
            longFieldSetter = makeConstructorSubSetter(longFieldSetter, f, char.class, x -> (char) (long) x);

            SubSetter doubleFieldSetter = null;
            doubleFieldSetter = makeConstructorSubSetter(doubleFieldSetter, f, double.class, x -> (double) x);
            doubleFieldSetter = makeConstructorSubSetter(doubleFieldSetter, f, float.class, x -> (float) (double) x);

            SubSetter booleanFieldSetter = null;
            booleanFieldSetter = makeConstructorSubSetter(booleanFieldSetter, f, boolean.class, x -> (boolean) x);

            SubSetter stringFieldSetter = null;
            stringFieldSetter = makeConstructorSubSetter(stringFieldSetter, f, String.class, x -> (String) x);

            return new CoercingFieldSetter(
                    (o, key, v) -> setField(f, o, (Object) null),
                    longFieldSetter,
                    doubleFieldSetter,
                    booleanFieldSetter,
                    stringFieldSetter,
                    (o, key, v) -> setField(f, o, v)
            );
        }

        private static <PRIM> SubSetter makeConstructorSubSetter(SubSetter setter, Field f, Class<PRIM> primClass, Function<Object, PRIM> convert) {
            return setter != null ? setter : Arrays.stream(f.getType().getConstructors())
                    .filter(c -> c.getParameterCount() == 1 && (c.getParameterTypes()[0] == primClass || c.getParameterTypes()[0] == box(primClass)))
                    .findFirst()
                    .map(c -> (SubSetter) (o, key, v) -> {
                        try {
                            setField(f, o, c.newInstance(convert.apply(v)));
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            throw new IllegalArgumentException("can not make new instance of " + c.getDeclaringClass().getSimpleName() + " with arg (" + v + ")", e);
                        }
                    })
                    .orElse(null);
        }

        public CoercingFieldSetter(SubSetter nullFieldSetter, SubSetter longFieldSetter, SubSetter doubleFieldSetter, SubSetter boolFieldSetter, SubSetter stringFieldSetter, SubSetter objectFieldSetter) {
            this.nullFieldSetter   = nullFieldSetter;
            this.longFieldSetter   = longFieldSetter;
            this.doubleFieldSetter = doubleFieldSetter;
            this.boolFieldSetter   = boolFieldSetter;
            this.stringFieldSetter = stringFieldSetter;
            this.objectFieldSetter = objectFieldSetter;
        }

        @Override
        public void set(Object o, Object key, Object v) {
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
        private static void setField(Field f, Object o, Object v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }
        }

        private static void setField(Field f, Object o, long v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }
        }

        private static void setField(Field f, Object o, int v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }
        }

        private static void setField(Field f, Object o, double v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }
        }

        private static void setField(Field f, Object o, String v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }
        }

        private static void setField(Field f, Object o, boolean v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }
        }

        private static void setField(Field f, Object o, short v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }
        }

        private static void setField(Field f, Object o, byte v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }
        }

        private static void setField(Field f, Object o, char v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }
        }

        private static void setField(Field f, Object o, float v) {
            try {
                f.set(o, v);
            } catch (IllegalAccessException e) {
                throw new Error(e);
            }
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        private static void setCollectionElement(Object o, Object v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Object>) o).add(v);
        }

        private static void setCollectionElement(Object o, long v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Long>) o).add(v);
        }

        private static void setCollectionElement(Object o, int v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Integer>) o).add(v);
        }

        private static void setCollectionElement(Object o, double v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Double>) o).add(v);
        }

        private static void setCollectionElement(Object o, String v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<String>) o).add(v);
        }

        private static void setCollectionElement(Object o, boolean v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Boolean>) o).add(v);
        }

        private static void setCollectionElement(Object o, short v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Short>) o).add(v);
        }

        private static void setCollectionElement(Object o, byte v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Byte>) o).add(v);
        }

        private static void setCollectionElement(Object o, char v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Character>) o).add(v);
        }

        private static void setCollectionElement(Object o, float v) {
            if (!(o instanceof Collection)) {
                throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName());
            }
            ((Collection<Float>) o).add(v);
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        private static void setMapElement(Object o, Object key, Object v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Object>) o).put(key, v);
        }

        private static void setMapElement(Object o, Object key, long v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Long>) o).put(key, v);
        }

        private static void setMapElement(Object o, Object key, int v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Integer>) o).put(key, v);
        }

        private static void setMapElement(Object o, Object key, double v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Double>) o).put(key, v);
        }

        private static void setMapElement(Object o, Object key, String v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, String>) o).put(key, v);
        }

        private static void setMapElement(Object o, Object key, boolean v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Boolean>) o).put(key, v);
        }

        private static void setMapElement(Object o, Object key, short v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Short>) o).put(key, v);
        }

        private static void setMapElement(Object o, Object key, byte v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Byte>) o).put(key, v);
        }

        private static void setMapElement(Object o, Object key, char v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Character>) o).put(key, v);
        }

        private static void setMapElement(Object o, Object key, float v) {
            if (!(o instanceof Map)) {
                throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName());
            }
            ((Map<Object, Float>) o).put(key, v);
        }
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
}
