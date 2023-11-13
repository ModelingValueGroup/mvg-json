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
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.modelingvalue.json.U.RW;

interface TypeInfo {
    static TypeInfo makeTypeInfo(Type type, Config config, Consumer<TypeInfo> topStackReplacer) {
        Class<?> rawClass = U.getRawClassOf(type);

        if (rawClass == null) {
            throw new RuntimeException("cannot make TypeInfo of <null>");
        } else if (Map.class.isAssignableFrom(rawClass)) {
            return new MapTypeInfo(rawClass, U.getElementType(type), config);
        } else if (Collection.class.isAssignableFrom(rawClass)) {
            return new CollectionTypeInfo(rawClass, U.getElementType(type), config);
        } else if (rawClass.isRecord()) {
            return new RecordTypeInfo(rawClass, config);
        } else if (rawClass.isArray()) {
            throw new RuntimeException("Arrays not yet supported: " + type);
        } else if (Object.class.isAssignableFrom(rawClass)) {
            Method classSelector = Arrays.stream(rawClass.getMethods())//
                    .filter(m -> config.getAnnotation(m, JsonClassSelector.class) != null)//
                    .filter(m -> Modifier.isStatic(m.getModifiers()))//
                    .filter(m -> Class.class.isAssignableFrom(m.getReturnType()))//
                    .filter(m -> m.getParameterCount() == 2)//
                    .filter(m -> m.getParameterTypes()[0] == String.class)//
                    .findFirst()//
                    .orElse(null);
            if (classSelector != null) {
                return new SelectorTypeInfo(rawClass, classSelector, config, topStackReplacer);
            } else {
                return new ObjectTypeInfo(rawClass, config);
            }
        }
        throw new IllegalArgumentException("Class not yet supported: " + rawClass.getSimpleName());
    }

    Maker getMaker();

    FieldSetter getFieldSetter(Object key); // key can be String or Integer

    Type getFieldType(Object key);

    boolean isIdField(String name);

    Object convert(Object m);

    interface Maker {
        Object make();

        private static Maker of(Class<?> clazz) {
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
            if (Modifier.isAbstract(clazz.getModifiers())) {
                throw new RuntimeException("problem instanciating abstract class " + clazz.getSimpleName());
            }
            return () -> {
                try {
                    return clazz.getConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException("problem instanciating " + clazz.getSimpleName(), e);
                }
            };
        }

    }

    abstract class BaseTypeInfo implements TypeInfo {
        final Class<?>    clazz;
        final Maker       maker;
        final FieldSetter fieldSetter;
        final Type        fieldType;

        final Config      config;

        BaseTypeInfo(Class<?> clazz, Maker maker, FieldSetter fieldSetter, Type fieldType, Config config) {
            this.clazz = clazz;
            this.maker = maker;
            this.fieldSetter = fieldSetter;
            this.fieldType = fieldType;
            this.config = config;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + getClazz().getSimpleName() + "]";
        }

        public Class<?> getClazz() {
            return clazz;
        }

        @Override
        public Maker getMaker() {
            return maker;
        }

        @Override
        public FieldSetter getFieldSetter(Object key) {
            return fieldSetter;
        }

        @Override
        public Type getFieldType(Object key) {
            return fieldType;
        }

        @Override
        public boolean isIdField(String name) {
            return false;
        }

        @Override
        public Object convert(Object m) {
            return m;
        }
    }

    class CollectionTypeInfo extends BaseTypeInfo {
        public CollectionTypeInfo(Class<?> clazz, Type elementType, Config config) {
            super(clazz, Maker.of(clazz), CoercingMemberSetter.forCollection(elementType), elementType, config);
        }
    }

    class MapTypeInfo extends BaseTypeInfo {
        public MapTypeInfo(Class<?> clazz, Type elementType, Config config) {
            super(clazz, Maker.of(clazz), CoercingMemberSetter.forMap(elementType), elementType, config);
        }
    }

    class RecordTypeInfo extends BaseTypeInfo {
        private final Class<?>[]                      types;
        private final List<String>                    fieldNames;
        private final Map<String, ? extends Class<?>> fieldTypesMap;
        private final Map<String, FieldSetter>        fieldSettersMap;

        public RecordTypeInfo(Class<?> clazz, Config config) {
            super(clazz, HashMap::new, CoercingMemberSetter.forMap(Object.class), null, config);
            RecordComponent[] recordComponents = clazz.getRecordComponents();
            types = Arrays.stream(recordComponents).map(RecordComponent::getType).toArray(n -> new Class<?>[n]);
            fieldNames = Arrays.stream(recordComponents).map(RecordComponent::getName).toList();
            fieldTypesMap = Arrays.stream(recordComponents).collect(Collectors.toMap(RecordComponent::getName, RecordComponent::getType));
            fieldSettersMap = Arrays.stream(recordComponents).collect(Collectors.toMap(RecordComponent::getName, rc -> CoercingMemberSetter.forMap(rc.getType())));
        }

        @Override
        public FieldSetter getFieldSetter(Object key) {
            if (!(key instanceof String)) {
                throw new RuntimeException("unexpected key type (String expected): " + key.getClass().getSimpleName());
            }
            return fieldSettersMap.get(key);
        }

        @Override
        public Type getFieldType(Object key) {
            if (!(key instanceof String)) {
                throw new RuntimeException("unexpected key type (String expected): " + key.getClass().getSimpleName());
            }
            return fieldTypesMap.get(key);
        }

        @Override
        public Object convert(Object m) {
            if (!(m instanceof Map map)) {
                throw new RuntimeException("cannot convert " + m.getClass().getSimpleName() + " to " + clazz.getSimpleName());
            }
            try {
                Object[] values = fieldNames.stream().map(name -> map.get(name)).toArray();
                return clazz.getDeclaredConstructor(types).newInstance(values);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                String argTypes = Arrays.stream(types).map(Class::getSimpleName).collect(Collectors.joining(","));
                throw new RuntimeException("could not make record: " + clazz.getSimpleName() + "(" + argTypes + ")", e);
            }
        }
    }

    class SelectorTypeInfo extends BaseTypeInfo {
        private final Method             classSelector;
        private final Consumer<TypeInfo> topStackReplacer;

        public SelectorTypeInfo(Class<?> clazz, Method classSelector, Config config, Consumer<TypeInfo> topStackReplacer) {
            super(clazz, () -> null, null, null, config);
            this.classSelector = classSelector;
            this.topStackReplacer = topStackReplacer;
        }

        @Override
        public FieldSetter getFieldSetter(Object key_) {
            return (o, key, v) -> {
                assert o == null;
                assert key == key_;
                try {
                    Object clazzObj = classSelector.invoke(null, key, v);
                    if (!(clazzObj instanceof Class<?> clazzClass)) {
                        throw new RuntimeException("problem in class-selector " + classSelector + ", it returned " + clazzObj);
                    }
                    ObjectTypeInfo newTypeInfo = new ObjectTypeInfo(clazzClass, config);
                    topStackReplacer.accept(newTypeInfo);
                    Object newObject = clazzClass.getDeclaredConstructor().newInstance();
                    return newTypeInfo.getMemberInfo(key).fieldSetter.set(newObject, key, v);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
                    throw new RuntimeException("problem in class-selector " + classSelector, e);
                }
            };
        }
    }

    class ObjectTypeInfo extends BaseTypeInfo {
        public static class MemberInfo {
            final Type        type;
            final FieldSetter fieldSetter;
            final boolean     isId;
            final String      name;

            public MemberInfo(Type type, FieldSetter fieldSetter, boolean isId, String name) {
                this.type = type;
                this.fieldSetter = fieldSetter;
                this.isId = isId;
                this.name = name;
            }
        }

        public class MethodMemberInfo extends MemberInfo {
            public final Method method;

            public MethodMemberInfo(Method m) {
                super(m.getGenericParameterTypes()[0], CoercingMemberSetter.forMethod(m, config.ignoreSFOs), config.getAnnotation(m, JsonId.class) != null, U.methodToElementName(m));
                method = m;
            }
        }

        public class FieldMemberInfo extends MemberInfo {
            public final Field field;

            private FieldMemberInfo(Field f) {
                super(f.getGenericType(), CoercingMemberSetter.forField(f, config.ignoreSFOs), config.getAnnotation(f, JsonId.class) != null, U.fieldToElementName(f, config));
                field = f;
            }
        }

        private final Map<String, MemberInfo> memberMap = new HashMap<>();
        private final String                  idFieldName;

        public ObjectTypeInfo(Class<?> clazz, Config config) {
            super(clazz, Maker.of(clazz), null, null, config);
            U.forAllMethodsAndFields(clazz, config, //
                    m -> memberMap.put(U.methodToElementName(m), new MethodMemberInfo(m)), //
                    f -> memberMap.put(U.fieldToElementName(f, config), new FieldMemberInfo(f)), //
                    RW.WRITE);
            idFieldName = memberMap.values().stream().filter(mi -> mi.isId).findFirst().map(mi -> mi.name).orElse(null);
        }

        @Override
        public Type getFieldType(Object key) {
            return getMemberInfo(key).type;
        }

        @Override
        public FieldSetter getFieldSetter(Object key) {
            return getMemberInfo(key).fieldSetter;
        }

        private MemberInfo getMemberInfo(Object key) {
            MemberInfo mi = getMemberInfoOrNull(key);
            if (mi == null) {
                throw new RuntimeException("the type " + getClazz().getSimpleName() + " does not have a field " + key);
            }
            return mi;
        }

        private MemberInfo getMemberInfoOrNull(Object key) {
            if (!(key instanceof String)) {
                throw new RuntimeException("unexpected key type (String expected): " + key.getClass().getSimpleName());
            }
            return memberMap.get((String) key);
        }

        @Override
        public boolean isIdField(String name) {
            return Objects.equals(name, idFieldName);
        }

        @Override
        public String toString() {
            return super.toString() + ":" + memberMap.keySet().stream().sorted().toList();
        }
    }
}
