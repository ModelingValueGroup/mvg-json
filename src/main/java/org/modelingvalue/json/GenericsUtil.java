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
import java.lang.reflect.Method;
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
    interface Maker {
        Object make();
    }

    interface SubSetter {
        // if (o==Collection) o.add(v) or o[key]=v (key=Integer)
        // if (o==Map       ) o.put(key,v)
        // if (o==Object    ) o.key = v
        Object set(Object o, Object key, Object v);
    }

    interface TypeInfo {
        Maker getMaker();

        SubSetter getSubSetter(Object key);

        Type getSubType(Object key);
    }

    public static TypeInfo makeTypeInfo(Type type, boolean ignoreSFOs) {
        Class<?> rawClass = getRawClassOf(type);

        if (Map.class.isAssignableFrom(rawClass)) {
            return new MapTypeInfo(rawClass, getElementType(type));
        } else if (Collection.class.isAssignableFrom(rawClass)) {
            return new CollectionTypeInfo(rawClass, getElementType(type));
        } else if (Object.class.isAssignableFrom(rawClass)) {
            return new ObjectTypeInfo(rawClass, ignoreSFOs);
        }
        throw new IllegalArgumentException("no TypeInfoMap for " + rawClass.getSimpleName() + " possible");
    }

    public abstract static class BaseTypeInfo implements TypeInfo {
        public final Class<?>  clazz;
        public final Maker     maker;
        public final SubSetter subSetter;
        public final Type      subType;

        public BaseTypeInfo(Class<?> clazz, Maker maker, SubSetter subSetter, Type subType) {
            this.clazz     = clazz;
            this.maker     = maker;
            this.subSetter = subSetter;
            this.subType   = subType;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public Maker getMaker() {
            return maker;
        }

        public SubSetter getSubSetter(Object key) {
            return subSetter;
        }

        public Type getSubType(Object key) {
            return subType;
        }

        protected static Maker defaultMaker(Class<?> clazz) {
            if (clazz.isInterface()) {
                if (clazz.isAssignableFrom(List.class)) {
                    return ArrayList::new;
                }
                if (clazz.isAssignableFrom(Set.class)) {
                    return HashSet::new;
                }
                if (clazz.isAssignableFrom(Map.class)) {
                    return HashMap::new;
                }
                throw new RuntimeException("problem instanciating interface " + clazz.getSimpleName());
            }
            return () -> {
                try {
                    return clazz.getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException("problem instanciating " + clazz.getSimpleName(), e);
                }
            };
        }
    }

    public static class CollectionTypeInfo extends BaseTypeInfo {
        public CollectionTypeInfo(Class<?> clazz, Type elementType) {
            super(clazz, defaultMaker(clazz), CoercingFieldSetter.forCollection(elementType), elementType);
        }

        @Override
        public String toString() {
            return "CollectionTypeInfo[" + getClazz().getSimpleName() + "]";
        }
    }

    public static class MapTypeInfo extends BaseTypeInfo {
        public MapTypeInfo(Class<?> clazz, Type elementType) {
            super(clazz, defaultMaker(clazz), CoercingFieldSetter.forMap(elementType), elementType);
        }

        @Override
        public String toString() {
            return "MapTypeInfo[" + getClazz().getSimpleName() + "]";
        }
    }

    public static class ObjectTypeInfo extends BaseTypeInfo {
        public class FieldInfo {
            public final Type      type;
            public final SubSetter subSetter;

            private FieldInfo(Field f) {
                this.type      = f.getGenericType();
                this.subSetter = CoercingFieldSetter.forField(f, ignoreSFOs);
            }
        }

        private final Method                                classSelector;
        private final boolean                               ignoreSFOs;
        private final Map<Class<?>, Map<String, FieldInfo>> fieldInfoMapMap = new HashMap<>();

        public ObjectTypeInfo(Class<?> clazz, boolean ignoreSFOs) {
            super(clazz, defaultMaker(clazz), null, null);
            this.classSelector = findClassSelector(clazz);
            this.ignoreSFOs    = ignoreSFOs;
            addFieldInfoMapOf(clazz);
        }

        private static Method findClassSelector(Class<?> clazz) {
            return Arrays.stream(clazz.getMethods())
                    .filter(m -> m.getAnnotation(JsonClassSelector.class) != null)
                    .filter(m -> Modifier.isStatic(m.getModifiers()))
                    .filter(m -> Class.class.isAssignableFrom(m.getReturnType()))
                    .filter(m -> m.getParameterCount() == 2)
                    .filter(m -> m.getParameterTypes()[0] == String.class)
                    .findFirst()
                    .orElse(null);
        }

        private void addFieldInfoMapOf(Class<?> clazz) {
            fieldInfoMapMap.computeIfAbsent(clazz, this::makeFieldInfoMap);
        }

        private Map<String, FieldInfo> makeFieldInfoMap(Class<?> clazz) {
            return getAllFields(clazz).stream()
                    .peek(f -> f.setAccessible(true))
                    .filter(f -> !f.isSynthetic() && !Modifier.isFinal(f.getModifiers()) && !Modifier.isStatic(f.getModifiers()))
                    .collect(Collectors.toMap(GenericsUtil::getFieldTag, FieldInfo::new));
        }

        private static List<Field> getAllFields(Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                fields.addAll(Arrays.asList(c.getDeclaredFields()));
            }
            return fields;
        }

        @Override
        public Maker getMaker() {
            return classSelector == null ? super.getMaker() : () -> null;
        }

        @Override
        public Type getSubType(Object key) {
            return getFieldInfo(getClazz(), key).type;
        }

        public SubSetter getSubSetter(Object key) {
            if (classSelector == null) {
                return getFieldInfo(getClazz(), key).subSetter;
            } else {
                return (o, key_, v) -> {
                    if (o != null) {
                        return getFieldInfo(o.getClass(), key).subSetter.set(o, key_, v);
                    }
                    try {
                        Object clazzObj = classSelector.invoke(null, key_, v);
                        if (!(clazzObj instanceof Class)) {
                            throw new RuntimeException("problem in class-selector " + classSelector + ", it returned " + clazzObj);
                        }
                        Class<?> clazz = (Class<?>) clazzObj;
                        addFieldInfoMapOf(clazz);
                        Object newObject = clazz.getDeclaredConstructor().newInstance();
                        return getFieldInfo(clazz, key).subSetter.set(newObject, key_, v);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                             InstantiationException e) {
                        throw new RuntimeException("problem in class-selector " + classSelector, e);
                    }
                };
            }
        }

        private FieldInfo getFieldInfoOrNull(Class<?> clazz, Object key) {
            if (!(key instanceof String)) {
                throw new RuntimeException("unexpected key type (String expected): " + key.getClass().getSimpleName());
            }
            return fieldInfoMapMap.get(clazz).get((String) key);
        }

        private FieldInfo getFieldInfo(Class<?> clazz, Object key) {
            FieldInfo fieldInfo = getFieldInfoOrNull(clazz, key);
            if (fieldInfo == null) {
                throw new RuntimeException("the type " + clazz.getSimpleName() + " does not have a field " + key);
            }
            return fieldInfo;
        }

        @Override
        public String toString() {
            int           numMaps  = fieldInfoMapMap.size();
            List<Integer> mapSizes = fieldInfoMapMap.values().stream().map(Map::size).collect(Collectors.toList());
            return "ObjectTypeInfo[" + getClazz().getSimpleName() + " with " + numMaps + " subs with " + mapSizes + " fields]";
        }
    }

    private static String getFieldTag(Field f) {
        JsonName nameAnno = f.getAnnotation(JsonName.class);
        return nameAnno == null ? f.getName() : nameAnno.value();
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
        @SuppressWarnings({"FieldCanBeLocal", "unused"})
        private final String    name;
        private final SubSetter nullFieldSetter;
        private final SubSetter longFieldSetter;
        private final SubSetter doubleFieldSetter;
        private final SubSetter boolFieldSetter;
        private final SubSetter stringFieldSetter;
        private final SubSetter objectFieldSetter;

        public static SubSetter forCollection(Type subType) {
            Class<?> clazz = getRawClassOf(subType);

            if (clazz == long.class || clazz == Long.class) {
                return new CoercingFieldSetter("long array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Long) null),
                        (o, key, v) -> setCollectionElement(o, (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, Long.parseLong((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == double.class || clazz == Double.class) {
                return new CoercingFieldSetter("double array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Double) null),
                        (o, key, v) -> setCollectionElement(o, (double) (long) v),
                        (o, key, v) -> setCollectionElement(o, (double) v),
                        null,
                        (o, key, v) -> setCollectionElement(o, Double.parseDouble((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == int.class || clazz == Integer.class) {
                return new CoercingFieldSetter("int array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Integer) null),
                        (o, key, v) -> setCollectionElement(o, (int) (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, Integer.parseInt((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == boolean.class || clazz == Boolean.class) {
                return new CoercingFieldSetter("boolean array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Boolean) null),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, (boolean) v),
                        (o, key, v) -> setCollectionElement(o, Boolean.parseBoolean((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == byte.class || clazz == Byte.class) {
                return new CoercingFieldSetter("byte array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Byte) null),
                        (o, key, v) -> setCollectionElement(o, (byte) (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, Byte.parseByte((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == short.class || clazz == Short.class) {
                return new CoercingFieldSetter("short array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Short) null),
                        (o, key, v) -> setCollectionElement(o, (short) (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, Short.parseShort((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == char.class || clazz == Character.class) {
                return new CoercingFieldSetter("char array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Character) null),
                        (o, key, v) -> setCollectionElement(o, (char) (long) v),
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, ((String) v).charAt(0)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == float.class || clazz == Float.class) {
                return new CoercingFieldSetter("float array",
                        clazz.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Float) null),
                        (o, key, v) -> setCollectionElement(o, (float) (long) v),
                        (o, key, v) -> setCollectionElement(o, (float) (double) v),
                        null,
                        (o, key, v) -> setCollectionElement(o, Float.parseFloat((String) v)),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == String.class) {
                return new CoercingFieldSetter("String array",
                        (o, key, v) -> setCollectionElement(o, null),
                        (o, key, v) -> setCollectionElement(o, Long.toString((long) v)),
                        (o, key, v) -> setCollectionElement(o, Double.toString((double) v)),
                        (o, key, v) -> setCollectionElement(o, Boolean.toString((boolean) v)),
                        (o, key, v) -> setCollectionElement(o, (String) v),
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == Iterable.class) {
                return new CoercingFieldSetter("Iterable array",
                        (o, key, v) -> setCollectionElement(o, (Iterable<?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            if (clazz == Map.class) {
                return new CoercingFieldSetter("Map array",
                        (o, key, v) -> setCollectionElement(o, (Map<?, ?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setCollectionElement(o, v)
                );
            }
            return new CoercingFieldSetter("other array",
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
                return new CoercingFieldSetter("long Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Long) null),
                        (o, key, v) -> setMapElement(o, key, (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, Long.parseLong((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == double.class || clazz == Double.class) {
                return new CoercingFieldSetter("double Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Double) null),
                        (o, key, v) -> setMapElement(o, key, (double) (long) v),
                        (o, key, v) -> setMapElement(o, key, (double) v),
                        null,
                        (o, key, v) -> setMapElement(o, key, Double.parseDouble((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == int.class || clazz == Integer.class) {
                return new CoercingFieldSetter("int Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Integer) null),
                        (o, key, v) -> setMapElement(o, key, (int) (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, Integer.parseInt((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == boolean.class || clazz == Boolean.class) {
                return new CoercingFieldSetter("boolean Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Boolean) null),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, (boolean) v),
                        (o, key, v) -> setMapElement(o, key, Boolean.parseBoolean((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == byte.class || clazz == Byte.class) {
                return new CoercingFieldSetter("long Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Byte) null),
                        (o, key, v) -> setMapElement(o, key, (byte) (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, Byte.parseByte((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == short.class || clazz == Short.class) {
                return new CoercingFieldSetter("short Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Short) null),
                        (o, key, v) -> setMapElement(o, key, (short) (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, Short.parseShort((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == char.class || clazz == Character.class) {
                return new CoercingFieldSetter("char Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Character) null),
                        (o, key, v) -> setMapElement(o, key, (char) (long) v),
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, ((String) v).charAt(0)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == float.class || clazz == Float.class) {
                return new CoercingFieldSetter("float Map",
                        clazz.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Float) null),
                        (o, key, v) -> setMapElement(o, key, (float) (long) v),
                        (o, key, v) -> setMapElement(o, key, (float) (double) v),
                        null,
                        (o, key, v) -> setMapElement(o, key, Float.parseFloat((String) v)),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == String.class) {
                return new CoercingFieldSetter("String Map",
                        (o, key, v) -> setMapElement(o, key, null),
                        (o, key, v) -> setMapElement(o, key, Long.toString((long) v)),
                        (o, key, v) -> setMapElement(o, key, Double.toString((double) v)),
                        (o, key, v) -> setMapElement(o, key, Boolean.toString((boolean) v)),
                        (o, key, v) -> setMapElement(o, key, (String) v),
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == Iterable.class) {
                return new CoercingFieldSetter("Iterable Map",
                        (o, key, v) -> setMapElement(o, key, (Iterable<?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            if (clazz == Map.class) {
                return new CoercingFieldSetter("Map Map",
                        (o, key, v) -> setMapElement(o, key, (Map<?, ?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setMapElement(o, key, v)
                );
            }
            return new CoercingFieldSetter("other Map",
                    (o, key, v) -> setMapElement(o, key, (Object) null),
                    (o, key, v) -> setMapElement(o, key, v),
                    (o, key, v) -> setMapElement(o, key, v),
                    (o, key, v) -> setMapElement(o, key, v),
                    (o, key, v) -> setMapElement(o, key, v),
                    (o, key, v) -> setMapElement(o, key, v)
            );
        }

        private static CoercingFieldSetter forField(Field f, boolean ignoreSFO) {
            Class<?> clazz = f.getType();
            if (clazz == long.class || clazz == Long.class) {
                return new CoercingFieldSetter("long field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Long) null),
                        (o, key, v) -> setField(f, o, (long) v),
                        (o, key, v) -> setField(f, o, (long) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Long.parseLong((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == double.class || clazz == Double.class) {
                return new CoercingFieldSetter("double field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Double) null),
                        (o, key, v) -> setField(f, o, (double) (long) v),
                        (o, key, v) -> setField(f, o, (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Double.parseDouble((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == int.class || clazz == Integer.class) {
                return new CoercingFieldSetter("int field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Integer) null),
                        (o, key, v) -> setField(f, o, (int) (long) v),
                        (o, key, v) -> setField(f, o, (int) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Integer.parseInt((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == boolean.class || clazz == Boolean.class) {
                return new CoercingFieldSetter("boolean field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Boolean) null),
                        null,
                        null,
                        (o, key, v) -> setField(f, o, (boolean) v),
                        (o, key, v) -> setField(f, o, Boolean.parseBoolean((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == byte.class || clazz == Byte.class) {
                return new CoercingFieldSetter("byte field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Byte) null),
                        (o, key, v) -> setField(f, o, (byte) (long) v),
                        (o, key, v) -> setField(f, o, (byte) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Byte.parseByte((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == short.class || clazz == Short.class) {
                return new CoercingFieldSetter("short field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Short) null),
                        (o, key, v) -> setField(f, o, (short) (long) v),
                        (o, key, v) -> setField(f, o, (short) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Short.parseShort((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == char.class || clazz == Character.class) {
                return new CoercingFieldSetter("char field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Character) null),
                        (o, key, v) -> setField(f, o, (char) (long) v),
                        null,
                        null,
                        (o, key, v) -> setField(f, o, ((String) v).charAt(0)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == float.class || clazz == Float.class) {
                return new CoercingFieldSetter("float field " + f.getName(),
                        clazz.isPrimitive() ? null : (o, key, v) -> setField(f, o, (Float) null),
                        (o, key, v) -> setField(f, o, (float) (long) v),
                        (o, key, v) -> setField(f, o, (float) (double) v),
                        null,
                        (o, key, v) -> setField(f, o, Float.parseFloat((String) v)),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == String.class) {
                return new CoercingFieldSetter("String field " + f.getName(),
                        (o, key, v) -> setField(f, o, null),
                        (o, key, v) -> setField(f, o, Long.toString((long) v)),
                        (o, key, v) -> setField(f, o, Double.toString((double) v)),
                        (o, key, v) -> setField(f, o, Boolean.toString((boolean) v)),
                        (o, key, v) -> setField(f, o, (String) v),
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == Iterable.class) {
                return new CoercingFieldSetter("Iterable field " + f.getName(),
                        (o, key, v) -> setField(f, o, (Iterable<?>) null),
                        null,
                        null,
                        null,
                        null,
                        (o, key, v) -> setField(f, o, v)
                );
            }
            if (clazz == Map.class) {
                return new CoercingFieldSetter("Map field " + f.getName(),
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
                longFieldSetter    = makeConstructorSubSetter(longFieldSetter, f, long.class, x -> (long) x);
                longFieldSetter    = makeConstructorSubSetter(longFieldSetter, f, int.class, x -> (int) (long) x);
                longFieldSetter    = makeConstructorSubSetter(longFieldSetter, f, short.class, x -> (short) (long) x);
                longFieldSetter    = makeConstructorSubSetter(longFieldSetter, f, byte.class, x -> (byte) (long) x);
                longFieldSetter    = makeConstructorSubSetter(longFieldSetter, f, char.class, x -> (char) (long) x);
                doubleFieldSetter  = makeConstructorSubSetter(doubleFieldSetter, f, double.class, x -> (double) x);
                doubleFieldSetter  = makeConstructorSubSetter(doubleFieldSetter, f, float.class, x -> (float) (double) x);
                booleanFieldSetter = makeConstructorSubSetter(booleanFieldSetter, f, boolean.class, x -> (boolean) x);
                stringFieldSetter  = makeConstructorSubSetter(stringFieldSetter, f, String.class, x -> (String) x);
            }

            return new CoercingFieldSetter("other field " + f.getName(),
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
                            return o;
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                            throw new IllegalArgumentException("can not make new instance of " + c.getDeclaringClass().getSimpleName() + " with arg (" + v + ")", e);
                        }
                    })
                    .orElse(null);
        }

        public CoercingFieldSetter(String name, SubSetter nullFieldSetter, SubSetter longFieldSetter, SubSetter doubleFieldSetter, SubSetter boolFieldSetter, SubSetter stringFieldSetter, SubSetter objectFieldSetter) {
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
}
