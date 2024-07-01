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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

abstract class TypeInfo {
    private static final boolean TRACE = Boolean.getBoolean("JSON.TYPE_INFO.TRACE");

    static TypeInfo makeTypeInfo(Type type, Config config, Consumer<TypeInfo> topStackReplacer) {
        TypeInfo result;
        Class<?> rawClass = U.getRawClassOf(type);

        if (rawClass == null) {
            throw new RuntimeException("cannot make TypeInfo of <null>");
        } else if (Map.class.isAssignableFrom(rawClass)) {
            result = new MapTypeInfo(rawClass, U.getElementType(type), config);
        } else if (Collection.class.isAssignableFrom(rawClass)) {
            result = new CollectionTypeInfo(rawClass, U.getElementType(type), config);
        } else if (rawClass.isRecord()) {
            result = new RecordTypeInfo(rawClass, config);
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
                result = new SelectorTypeInfo(rawClass, classSelector, config, topStackReplacer);
            } else {
                result = new ObjectTypeInfo(rawClass, config);
            }
        } else {
            throw new IllegalArgumentException("Class not yet supported: " + rawClass.getSimpleName());
        }
        if (TRACE) {
            System.out.printf("TypeInfo[%-24s]: %s\n", rawClass.getSimpleName(), result.getClass().getSimpleName());
            result.getPropertyNames().forEach(n -> System.out.printf("   - %-20s : %-55s : %s\n", n, result.getPropertyType(n), result.getPropertySetter(n)));
        }
        return result;
    }

    abstract Maker getMaker();

    abstract List<String> getPropertyNames();

    abstract PropertySetter getPropertySetter(Object key); // key can be String or Integer

    abstract Type getPropertyType(Object key); // key can be String or Integer

    abstract boolean isIdProperty(String name);

    abstract Object convert(Object m);

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
                throw new RuntimeException("problem instantiating interface " + clazz.getSimpleName());
            }
            if (Modifier.isAbstract(clazz.getModifiers())) {
                throw new RuntimeException("problem instantiating abstract class " + clazz.getSimpleName());
            }
            return () -> {
                try {
                    return clazz.getConstructor().newInstance();
                } catch (InstantiationException |
                         IllegalAccessException |
                         InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException("problem instantiating " + clazz.getSimpleName(), e);
                }
            };
        }

    }

    static abstract class BaseTypeInfo extends TypeInfo {
        final Class<?>       clazz;
        final Maker          maker;
        final PropertySetter propertySetter;
        final Type           fieldType;

        final Config config;

        BaseTypeInfo(Class<?> clazz, Maker maker, PropertySetter propertySetter, Type fieldType, Config config) {
            this.clazz          = clazz;
            this.maker          = maker;
            this.propertySetter = propertySetter;
            this.fieldType      = fieldType;
            this.config         = config;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + getClazz().getSimpleName() + "]";
        }

        public Class<?> getClazz() {
            return clazz;
        }

        @Override
        Maker getMaker() {
            return maker;
        }

        @Override
        List<String> getPropertyNames() {
            return List.of();
        }

        @Override
        PropertySetter getPropertySetter(Object key) {
            return propertySetter;
        }

        @Override
        Type getPropertyType(Object key) {
            return fieldType;
        }

        @Override
        boolean isIdProperty(String name) {
            return false;
        }

        @Override
        Object convert(Object m) {
            return m;
        }
    }

    static class CollectionTypeInfo extends BaseTypeInfo {
        public CollectionTypeInfo(Class<?> clazz, Type elementType, Config config) {
            super(clazz, Maker.of(clazz), CoercingPropertySetter.forCollection(elementType), elementType, config);
        }
    }

    static class MapTypeInfo extends BaseTypeInfo {
        public MapTypeInfo(Class<?> clazz, Type elementType, Config config) {
            super(clazz, Maker.of(clazz), CoercingPropertySetter.forMap(elementType), elementType, config);
        }
    }

    static class RecordTypeInfo extends BaseTypeInfo {
        private final Class<?>[]                      types;
        private final List<String>                    fieldNames;
        private final Map<String, ? extends Class<?>> fieldTypesMap;
        private final Map<String, PropertySetter>     fieldSettersMap;

        public RecordTypeInfo(Class<?> clazz, Config config) {
            super(clazz, HashMap::new, CoercingPropertySetter.forMap(Object.class), null, config);
            RecordComponent[] recordComponents = clazz.getRecordComponents();
            types           = Arrays.stream(recordComponents).map(RecordComponent::getType).toArray(n -> new Class<?>[n]);
            fieldNames      = Arrays.stream(recordComponents).map(RecordComponent::getName).toList();
            fieldTypesMap   = Arrays.stream(recordComponents).collect(Collectors.toMap(RecordComponent::getName, RecordComponent::getType));
            fieldSettersMap = Arrays.stream(recordComponents).collect(Collectors.toMap(RecordComponent::getName, rc -> CoercingPropertySetter.forMap(rc.getType())));
        }

        List<String> getPropertyNames() {
            return fieldNames;
        }

        @Override
        PropertySetter getPropertySetter(Object key) {
            if (!(key instanceof String)) {
                throw new RuntimeException("unexpected key type (String expected): " + key.getClass().getSimpleName());
            }
            return fieldSettersMap.get(key);
        }

        @Override
        public Type getPropertyType(Object key) {
            if (!(key instanceof String)) {
                throw new RuntimeException("unexpected key type (String expected): " + key.getClass().getSimpleName());
            }
            return fieldTypesMap.get(key);
        }

        @Override
        public Object convert(Object m) {
            if (!(m instanceof Map<?, ?> map)) {
                throw new RuntimeException("cannot convert " + m.getClass().getSimpleName() + " to " + clazz.getSimpleName());
            }
            try {
                Object[] values = fieldNames.stream().map(map::get).toArray();
                return clazz.getDeclaredConstructor(types).newInstance(values);
            } catch (InstantiationException |
                     IllegalAccessException |
                     InvocationTargetException |
                     NoSuchMethodException e) {
                String argTypes = Arrays.stream(types).map(Class::getSimpleName).collect(Collectors.joining(","));
                throw new RuntimeException("could not make record: " + clazz.getSimpleName() + "(" + argTypes + ")", e);
            }
        }
    }

    static class SelectorTypeInfo extends BaseTypeInfo {
        private final Method             classSelector;
        private final Consumer<TypeInfo> topStackReplacer;

        public SelectorTypeInfo(Class<?> clazz, Method classSelector, Config config, Consumer<TypeInfo> topStackReplacer) {
            super(clazz, () -> null, null, null, config);
            this.classSelector    = classSelector;
            this.topStackReplacer = topStackReplacer;
        }

        @Override
        PropertySetter getPropertySetter(Object key_) {
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
                    return newTypeInfo.getPropertyAccessor(key).getCoercingPropertySetter(config).set(newObject, key, v);
                } catch (IllegalAccessException |
                         InvocationTargetException |
                         NoSuchMethodException |
                         InstantiationException e) {
                    throw new RuntimeException("problem in class-selector " + classSelector, e);
                }
            };
        }
    }

    static class ObjectTypeInfo extends BaseTypeInfo {
        private final Map<String, PropertyAccessor> name2paMap = new HashMap<>();
        private final String                        idFieldName;

        public ObjectTypeInfo(Class<?> clazz, Config config) {
            super(clazz, Maker.of(clazz), null, null, config);

            PropertyAccessor.all(clazz, config)
                            .forEach((name, pa) -> {
                                if (pa.canSet()) {
                                    name2paMap.put(pa.name(), pa);
                                }
                            });

            idFieldName = name2paMap.values().stream().filter(mi -> mi.isId(config)).findFirst().map(PropertyAccessor::name).orElse(null);
        }

        List<String> getPropertyNames() {
            return name2paMap.keySet().stream().toList();
        }

        @Override
        public Type getPropertyType(Object key) {
            return getPropertyAccessor(key).type();
        }

        @Override
        PropertySetter getPropertySetter(Object key) {
            return getPropertyAccessor(key).getCoercingPropertySetter(config);
        }

        private PropertyAccessor getPropertyAccessor(Object key) {
            PropertyAccessor pa = getPropertyAccessorOrNull(key);
            if (pa != null) {
                return pa;
            }
            throw new RuntimeException("the type " + getClazz().getSimpleName() + " does not have a property " + key);
        }

        private PropertyAccessor getPropertyAccessorOrNull(Object key) {
            if (key instanceof String s) {
                return name2paMap.get(s);
            }
            throw new RuntimeException("unexpected key type (String expected): " + key.getClass().getSimpleName());
        }

        @Override
        public boolean isIdProperty(String name) {
            return Objects.equals(name, idFieldName);
        }

        @Override
        public String toString() {
            return super.toString() + ":" + name2paMap.keySet().stream().sorted().toList();
        }
    }
}
