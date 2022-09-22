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

import org.modelingvalue.json.SubSetter.CoercingMemberSetter;
import org.modelingvalue.json.U.RW;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

interface TypeInfo {
    static TypeInfo makeTypeInfo(Type type, Config config, Consumer<TypeInfo> topStackReplacer) {
        Class<?> rawClass = U.getRawClassOf(type);

        if (Map.class.isAssignableFrom(rawClass)) {
            return new MapTypeInfo(rawClass, U.getElementType(type), config);
        } else if (Collection.class.isAssignableFrom(rawClass)) {
            return new CollectionTypeInfo(rawClass, U.getElementType(type), config);
        } else if (rawClass.isArray()) {
            throw new RuntimeException("Arrays not yet supported: " + type);
        } else if (Object.class.isAssignableFrom(rawClass)) {
            Method classSelector = Arrays.stream(rawClass.getMethods()).filter(m -> config.getAnnotation(m, JsonClassSelector.class) != null).filter(m -> Modifier.isStatic(m.getModifiers())).filter(m -> Class.class.isAssignableFrom(m.getReturnType())).filter(m -> m.getParameterCount() == 2).filter(m -> m.getParameterTypes()[0] == String.class).findFirst().orElse(null);
            if (classSelector != null) {
                return new SelectorTypeInfo(rawClass, classSelector, config, topStackReplacer);
            } else {
                return new ObjectTypeInfo(rawClass, config);
            }
        }
        throw new IllegalArgumentException("Class not yet supported: " + rawClass.getSimpleName());
    }

    Maker getMaker();

    SubSetter getSubSetter(Object key);

    Type getSubType(Object key);

    boolean isIdField(String name);

    interface Maker {
        Object make();
    }

    abstract class BaseTypeInfo implements TypeInfo {
        public final Class<?>  clazz;
        public final Maker     maker;
        public final SubSetter subSetter;
        public final Type      subType;
        public final Config    config;

        public BaseTypeInfo(Class<?> clazz, Maker maker, SubSetter subSetter, Type subType, Config config) {
            this.clazz     = clazz;
            this.maker     = maker;
            this.subSetter = subSetter;
            this.subType   = subType;
            this.config    = config;
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
            if (Modifier.isAbstract(clazz.getModifiers())) {
                throw new RuntimeException("problem instanciating abstract class " + clazz.getSimpleName());
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

        public boolean isIdField(String name) {
            return false;
        }
    }

    class CollectionTypeInfo extends BaseTypeInfo {
        public CollectionTypeInfo(Class<?> clazz, Type elementType, Config config) {
            super(clazz, defaultMaker(clazz), CoercingMemberSetter.forCollection(elementType), elementType, config);
        }

        @Override
        public String toString() {
            return "CollectionTypeInfo[" + getClazz().getSimpleName() + "]";
        }
    }

    class MapTypeInfo extends BaseTypeInfo {
        public MapTypeInfo(Class<?> clazz, Type elementType, Config config) {
            super(clazz, defaultMaker(clazz), CoercingMemberSetter.forMap(elementType), elementType, config);
        }

        @Override
        public String toString() {
            return "MapTypeInfo[" + getClazz().getSimpleName() + "]";
        }
    }

    class SelectorTypeInfo extends BaseTypeInfo {
        private final Method             classSelector;
        private final Consumer<TypeInfo> topStackReplacer;

        public SelectorTypeInfo(Class<?> clazz, Method classSelector, Config config, Consumer<TypeInfo> topStackReplacer) {
            super(clazz, () -> null, null, null, config);
            this.classSelector    = classSelector;
            this.topStackReplacer = topStackReplacer;
        }

        @Override
        public SubSetter getSubSetter(Object key_) {
            return (o, key, v) -> {
                assert o == null;
                assert key == key_;
                try {
                    Object clazzObj = classSelector.invoke(null, key, v);
                    if (!(clazzObj instanceof Class)) {
                        throw new RuntimeException("problem in class-selector " + classSelector + ", it returned " + clazzObj);
                    }
                    Class<?>       clazz       = (Class<?>) clazzObj;
                    ObjectTypeInfo newTypeInfo = new ObjectTypeInfo(clazz, config);
                    topStackReplacer.accept(newTypeInfo);
                    Object newObject = clazz.getDeclaredConstructor().newInstance();
                    return newTypeInfo.getMemberInfo(key).subSetter.set(newObject, key, v);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                         InstantiationException e) {
                    throw new RuntimeException("problem in class-selector " + classSelector, e);
                }
            };
        }

        @Override
        public String toString() {
            return "SelectorTypeInfo[" + getClazz().getSimpleName() + "]";
        }
    }

    class ObjectTypeInfo extends BaseTypeInfo {
        public static class MemberInfo {
            public final Type      type;
            public final SubSetter subSetter;
            public final boolean   isId;
            public final String    name;

            public MemberInfo(Type type, SubSetter subSetter, boolean isId, String name) {
                this.type      = type;
                this.subSetter = subSetter;
                this.isId      = isId;
                this.name      = name;
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
            super(clazz, defaultMaker(clazz), null, null, config);
            U.findElements(clazz, config,
                    m -> memberMap.put(U.methodToElementName(m), new MethodMemberInfo(m)),
                    f -> memberMap.put(U.fieldToElementName(f, config), new FieldMemberInfo(f)),
                    RW.WRITE);
            idFieldName = memberMap.values().stream().filter(mi -> mi.isId).findFirst().map(mi -> mi.name).orElse(null);
        }

        @Override
        public Type getSubType(Object key) {
            return getMemberInfo(key).type;
        }

        @Override
        public SubSetter getSubSetter(Object key) {
            return getMemberInfo(key).subSetter;
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

        public boolean isIdField(String name) {
            return Objects.equals(name, idFieldName);
        }

        @Override
        public String toString() {
            return "ObjectTypeInfo[" + getClazz().getSimpleName() + ":" + memberMap.keySet().stream().sorted().collect(Collectors.toList()) + "]";
        }
    }
}
