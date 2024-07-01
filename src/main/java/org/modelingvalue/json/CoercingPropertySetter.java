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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@SuppressWarnings({"unchecked", "Convert2MethodRef", "DuplicatedCode"})
class CoercingPropertySetter implements PropertySetter {
    static PropertySetter forCollection(Type elementType) {
        String         name;
        PropertySetter nullFs;
        PropertySetter longFs;
        PropertySetter doubleFs;
        PropertySetter boolFs;
        PropertySetter stringFs;
        PropertySetter objectFs;

        Class<?> elementClass = U.getRawClassOf(elementType);
        if (elementClass == long.class || elementClass == Long.class) {
            name     = "long";
            nullFs   = elementClass.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Long) null);
            longFs   = (o, key, v) -> setCollectionElement(o, (long) v);
            doubleFs = null;
            boolFs   = null;
            stringFs = (o, key, v) -> setCollectionElement(o, Long.parseLong((String) v));
            objectFs = (o, key, v) -> setCollectionElement(o, v);
        } else if (elementClass == double.class || elementClass == Double.class) {
            name     = "double";
            nullFs   = elementClass.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Double) null);
            longFs   = (o, key, v) -> setCollectionElement(o, (double) (long) v);
            doubleFs = (o, key, v) -> setCollectionElement(o, (double) v);
            boolFs   = null;
            stringFs = (o, key, v) -> setCollectionElement(o, Double.parseDouble((String) v));
            objectFs = (o, key, v) -> setCollectionElement(o, v);
        } else if (elementClass == int.class || elementClass == Integer.class) {
            name     = "int";
            nullFs   = elementClass.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Integer) null);
            longFs   = (o, key, v) -> setCollectionElement(o, (int) (long) v);
            doubleFs = null;
            boolFs   = null;
            stringFs = (o, key, v) -> setCollectionElement(o, Integer.parseInt((String) v));
            objectFs = (o, key, v) -> setCollectionElement(o, v);
        } else if (elementClass == boolean.class || elementClass == Boolean.class) {
            name     = "boolean";
            nullFs   = elementClass.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Boolean) null);
            longFs   = null;
            doubleFs = null;
            boolFs   = (o, key, v) -> setCollectionElement(o, (boolean) v);
            stringFs = (o, key, v) -> setCollectionElement(o, Boolean.parseBoolean((String) v));
            objectFs = (o, key, v) -> setCollectionElement(o, v);
        } else if (elementClass == byte.class || elementClass == Byte.class) {
            name     = "byte";
            nullFs   = elementClass.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Byte) null);
            longFs   = (o, key, v) -> setCollectionElement(o, (byte) (long) v);
            doubleFs = null;
            boolFs   = null;
            stringFs = (o, key, v) -> setCollectionElement(o, Byte.parseByte((String) v));
            objectFs = (o, key, v) -> setCollectionElement(o, v);
        } else if (elementClass == short.class || elementClass == Short.class) {
            name     = "short";
            nullFs   = elementClass.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Short) null);
            longFs   = (o, key, v) -> setCollectionElement(o, (short) (long) v);
            doubleFs = null;
            boolFs   = null;
            stringFs = (o, key, v) -> setCollectionElement(o, Short.parseShort((String) v));
            objectFs = (o, key, v) -> setCollectionElement(o, v);
        } else if (elementClass == char.class || elementClass == Character.class) {
            name     = "char";
            nullFs   = elementClass.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Character) null);
            longFs   = (o, key, v) -> setCollectionElement(o, (char) (long) v);
            doubleFs = null;
            boolFs   = null;
            stringFs = (o, key, v) -> setCollectionElement(o, ((String) v).charAt(0));
            objectFs = (o, key, v) -> setCollectionElement(o, v);
        } else if (elementClass == float.class || elementClass == Float.class) {
            name     = "float";
            nullFs   = elementClass.isPrimitive() ? null : (o, key, v) -> setCollectionElement(o, (Float) null);
            longFs   = (o, key, v) -> setCollectionElement(o, (float) (long) v);
            doubleFs = (o, key, v) -> setCollectionElement(o, (float) (double) v);
            boolFs   = null;
            stringFs = (o, key, v) -> setCollectionElement(o, Float.parseFloat((String) v));
            objectFs = (o, key, v) -> setCollectionElement(o, v);
        } else if (elementClass == String.class) {
            name     = "String";
            nullFs   = (o, key, v) -> setCollectionElement(o, null);
            longFs   = (o, key, v) -> setCollectionElement(o, Long.toString((long) v));
            doubleFs = (o, key, v) -> setCollectionElement(o, Double.toString((double) v));
            boolFs   = (o, key, v) -> setCollectionElement(o, Boolean.toString((boolean) v));
            stringFs = (o, key, v) -> setCollectionElement(o, (String) v);
            objectFs = (o, key, v) -> setCollectionElement(o, v);
        } else {
            name     = "other";
            nullFs   = (o, key, v) -> setCollectionElement(o, (Object) null);
            longFs   = null;
            doubleFs = null;
            boolFs   = null;
            stringFs = null;
            objectFs = (o, key, v) -> setCollectionElement(o, v);
        }
        return new CoercingPropertySetter(name + " []", nullFs, longFs, doubleFs, boolFs, stringFs, objectFs);
    }

    static PropertySetter forMap(Type mapValueType) {
        String         name;
        PropertySetter nullFs;
        PropertySetter longFs;
        PropertySetter doubleFs;
        PropertySetter boolFs;
        PropertySetter stringFs;
        PropertySetter objectFs;

        Class<?> valueClass = U.getRawClassOf(mapValueType);
        if (valueClass == long.class || valueClass == Long.class) {
            name     = "long";
            nullFs   = valueClass.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Long) null);
            longFs   = (o, key, v) -> setMapElement(o, key, (long) v);
            doubleFs = null;
            boolFs   = null;
            stringFs = (o, key, v) -> setMapElement(o, key, Long.parseLong((String) v));
            objectFs = (o, key, v) -> setMapElement(o, key, v);
        } else if (valueClass == double.class || valueClass == Double.class) {
            name     = "double";
            nullFs   = valueClass.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Double) null);
            longFs   = (o, key, v) -> setMapElement(o, key, (double) (long) v);
            doubleFs = (o, key, v) -> setMapElement(o, key, (double) v);
            boolFs   = null;
            stringFs = (o, key, v) -> setMapElement(o, key, Double.parseDouble((String) v));
            objectFs = (o, key, v) -> setMapElement(o, key, v);
        } else if (valueClass == int.class || valueClass == Integer.class) {
            name     = "int";
            nullFs   = valueClass.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Integer) null);
            longFs   = (o, key, v) -> setMapElement(o, key, (int) (long) v);
            doubleFs = null;
            boolFs   = null;
            stringFs = (o, key, v) -> setMapElement(o, key, Integer.parseInt((String) v));
            objectFs = (o, key, v) -> setMapElement(o, key, v);
        } else if (valueClass == boolean.class || valueClass == Boolean.class) {
            name     = "boolean";
            nullFs   = valueClass.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Boolean) null);
            longFs   = null;
            doubleFs = null;
            boolFs   = (o, key, v) -> setMapElement(o, key, (boolean) v);
            stringFs = (o, key, v) -> setMapElement(o, key, Boolean.parseBoolean((String) v));
            objectFs = (o, key, v) -> setMapElement(o, key, v);
        } else if (valueClass == byte.class || valueClass == Byte.class) {
            name     = "long";
            nullFs   = valueClass.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Byte) null);
            longFs   = (o, key, v) -> setMapElement(o, key, (byte) (long) v);
            doubleFs = null;
            boolFs   = null;
            stringFs = (o, key, v) -> setMapElement(o, key, Byte.parseByte((String) v));
            objectFs = (o, key, v) -> setMapElement(o, key, v);
        } else if (valueClass == short.class || valueClass == Short.class) {
            name     = "short";
            nullFs   = valueClass.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Short) null);
            longFs   = (o, key, v) -> setMapElement(o, key, (short) (long) v);
            doubleFs = null;
            boolFs   = null;
            stringFs = (o, key, v) -> setMapElement(o, key, Short.parseShort((String) v));
            objectFs = (o, key, v) -> setMapElement(o, key, v);
        } else if (valueClass == char.class || valueClass == Character.class) {
            name     = "char";
            nullFs   = valueClass.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Character) null);
            longFs   = (o, key, v) -> setMapElement(o, key, (char) (long) v);
            doubleFs = null;
            boolFs   = null;
            stringFs = (o, key, v) -> setMapElement(o, key, ((String) v).charAt(0));
            objectFs = (o, key, v) -> setMapElement(o, key, v);
        } else if (valueClass == float.class || valueClass == Float.class) {
            name     = "float";
            nullFs   = valueClass.isPrimitive() ? null : (o, key, v) -> setMapElement(o, key, (Float) null);
            longFs   = (o, key, v) -> setMapElement(o, key, (float) (long) v);
            doubleFs = (o, key, v) -> setMapElement(o, key, (float) (double) v);
            boolFs   = null;
            stringFs = (o, key, v) -> setMapElement(o, key, Float.parseFloat((String) v));
            objectFs = (o, key, v) -> setMapElement(o, key, v);
        } else if (valueClass == String.class) {
            name     = "String";
            nullFs   = (o, key, v) -> setMapElement(o, key, null);
            longFs   = (o, key, v) -> setMapElement(o, key, Long.toString((long) v));
            doubleFs = (o, key, v) -> setMapElement(o, key, Double.toString((double) v));
            boolFs   = (o, key, v) -> setMapElement(o, key, Boolean.toString((boolean) v));
            stringFs = (o, key, v) -> setMapElement(o, key, (String) v);
            objectFs = (o, key, v) -> setMapElement(o, key, v);
        } else {
            name     = "other";
            nullFs   = (o, key, v) -> setMapElement(o, key, (Object) null);
            longFs   = (o, key, v) -> setMapElement(o, key, v);
            doubleFs = (o, key, v) -> setMapElement(o, key, v);
            boolFs   = (o, key, v) -> setMapElement(o, key, v);
            stringFs = (o, key, v) -> setMapElement(o, key, v);
            objectFs = (o, key, v) -> setMapElement(o, key, v);
        }
        return new CoercingPropertySetter(name + " Map", nullFs, longFs, doubleFs, boolFs, stringFs, objectFs);
    }

    static CoercingPropertySetter forProperty(PropertyAccessor pa, Config config) {
        String         name;
        PropertySetter nullFs;
        PropertySetter longFs;
        PropertySetter doubleFs;
        PropertySetter boolFs;
        PropertySetter stringFs;
        PropertySetter objectFs;

        Class<?> clazz = pa.clazz();
        if (clazz == long.class || clazz == Long.class) {
            name     = "long";
            nullFs   = clazz.isPrimitive() ? null : (o, key, v) -> setProperty(pa, o, (Long) null);
            longFs   = (o, key, v) -> setProperty(pa, o, (long) v);
            doubleFs = (o, key, v) -> setProperty(pa, o, (long) (double) v);
            boolFs   = null;
            stringFs = (o, key, v) -> setProperty(pa, o, Long.parseLong((String) v));
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else if (clazz == double.class || clazz == Double.class) {
            name     = "double";
            nullFs   = clazz.isPrimitive() ? null : (o, key, v) -> setProperty(pa, o, (Double) null);
            longFs   = (o, key, v) -> setProperty(pa, o, (double) (long) v);
            doubleFs = (o, key, v) -> setProperty(pa, o, (double) v);
            boolFs   = null;
            stringFs = (o, key, v) -> setProperty(pa, o, Double.parseDouble((String) v));
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else if (clazz == int.class || clazz == Integer.class) {
            name     = "int";
            nullFs   = clazz.isPrimitive() ? null : (o, key, v) -> setProperty(pa, o, (Integer) null);
            longFs   = (o, key, v) -> setProperty(pa, o, (int) (long) v);
            doubleFs = (o, key, v) -> setProperty(pa, o, (int) (double) v);
            boolFs   = null;
            stringFs = (o, key, v) -> setProperty(pa, o, Integer.parseInt((String) v));
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else if (clazz == boolean.class || clazz == Boolean.class) {
            name     = "boolean";
            nullFs   = clazz.isPrimitive() ? null : (o, key, v) -> setProperty(pa, o, (Boolean) null);
            longFs   = null;
            doubleFs = null;
            boolFs   = (o, key, v) -> setProperty(pa, o, (boolean) v);
            stringFs = (o, key, v) -> setProperty(pa, o, Boolean.parseBoolean((String) v));
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else if (clazz == byte.class || clazz == Byte.class) {
            name     = "byte";
            nullFs   = clazz.isPrimitive() ? null : (o, key, v) -> setProperty(pa, o, (Byte) null);
            longFs   = (o, key, v) -> setProperty(pa, o, (byte) (long) v);
            doubleFs = (o, key, v) -> setProperty(pa, o, (byte) (double) v);
            boolFs   = null;
            stringFs = (o, key, v) -> setProperty(pa, o, Byte.parseByte((String) v));
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else if (clazz == short.class || clazz == Short.class) {
            name     = "short";
            nullFs   = clazz.isPrimitive() ? null : (o, key, v) -> setProperty(pa, o, (Short) null);
            longFs   = (o, key, v) -> setProperty(pa, o, (short) (long) v);
            doubleFs = (o, key, v) -> setProperty(pa, o, (short) (double) v);
            boolFs   = null;
            stringFs = (o, key, v) -> setProperty(pa, o, Short.parseShort((String) v));
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else if (clazz == char.class || clazz == Character.class) {
            name     = "char";
            nullFs   = clazz.isPrimitive() ? null : (o, key, v) -> setProperty(pa, o, (Character) null);
            longFs   = (o, key, v) -> setProperty(pa, o, (char) (long) v);
            doubleFs = null;
            boolFs   = null;
            stringFs = (o, key, v) -> setProperty(pa, o, ((String) v).charAt(0));
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else if (clazz == float.class || clazz == Float.class) {
            name     = "float";
            nullFs   = clazz.isPrimitive() ? null : (o, key, v) -> setProperty(pa, o, (Float) null);
            longFs   = (o, key, v) -> setProperty(pa, o, (float) (long) v);
            doubleFs = (o, key, v) -> setProperty(pa, o, (float) (double) v);
            boolFs   = null;
            stringFs = (o, key, v) -> setProperty(pa, o, Float.parseFloat((String) v));
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else if (clazz == String.class) {
            name     = "String";
            nullFs   = (o, key, v) -> setProperty(pa, o, null);
            longFs   = (o, key, v) -> setProperty(pa, o, Long.toString((long) v));
            doubleFs = (o, key, v) -> setProperty(pa, o, Double.toString((double) v));
            boolFs   = (o, key, v) -> setProperty(pa, o, Boolean.toString((boolean) v));
            stringFs = (o, key, v) -> setProperty(pa, o, (String) v);
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else if (Set.class.isAssignableFrom(clazz)) {
            Class<?> argClass = getOneTypeArgClass(pa);
            name     = "Set";
            nullFs   = (o, key, v) -> setProperty(pa, o, (Iterable<?>) null);
            longFs   = argClass == Long.class ? (o, key, v) -> addToSetProperty(pa, o, v) : null;
            doubleFs = argClass == Double.class ? (o, key, v) -> addToSetProperty(pa, o, v) : null;
            boolFs   = argClass == Boolean.class ? (o, key, v) -> addToSetProperty(pa, o, v) : null;
            stringFs = argClass == String.class ? (o, key, v) -> addToSetProperty(pa, o, v) : null;
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else if (List.class.isAssignableFrom(clazz)) {
            Class<?> argClass = getOneTypeArgClass(pa);
            name     = "List";
            nullFs   = (o, key, v) -> setProperty(pa, o, (Iterable<?>) null);
            longFs   = argClass == Long.class ? (o, key, v) -> addToListProperty(pa, o, v) : null;
            doubleFs = argClass == Double.class ? (o, key, v) -> addToListProperty(pa, o, v) : null;
            boolFs   = argClass == Boolean.class ? (o, key, v) -> addToListProperty(pa, o, v) : null;
            stringFs = argClass == String.class ? (o, key, v) -> addToListProperty(pa, o, v) : null;
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else if (Map.class.isAssignableFrom(clazz)) {
            name     = "Map";
            nullFs   = (o, key, v) -> setProperty(pa, o, (Map<?, ?>) null);
            longFs   = null;
            doubleFs = null;
            boolFs   = null;
            stringFs = null;
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else if (clazz == Object.class) {
            name     = "Object";
            nullFs   = (o, key, v) -> setProperty(pa, o, v);
            longFs   = (o, key, v) -> setProperty(pa, o, v);
            doubleFs = (o, key, v) -> setProperty(pa, o, v);
            boolFs   = (o, key, v) -> setProperty(pa, o, v);
            stringFs = (o, key, v) -> setProperty(pa, o, v);
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else if (!config.ignoreSFOs) {
            PropertySetter tmp;
            name     = "Constructor";
            nullFs   = (o, key, v) -> setProperty(pa, o, (Object) null);
            tmp      = makeConstructorFieldSetter(null, pa, long.class, x -> (long) x);
            tmp      = makeConstructorFieldSetter(tmp, pa, int.class, x -> (int) (long) x);
            tmp      = makeConstructorFieldSetter(tmp, pa, short.class, x -> (short) (long) x);
            tmp      = makeConstructorFieldSetter(tmp, pa, byte.class, x -> (byte) (long) x);
            longFs   = makeConstructorFieldSetter(tmp, pa, char.class, x -> (char) (long) x);
            tmp      = makeConstructorFieldSetter(null, pa, double.class, x -> (double) x);
            doubleFs = makeConstructorFieldSetter(tmp, pa, float.class, x -> (float) (double) x);
            boolFs   = makeConstructorFieldSetter(null, pa, boolean.class, x -> (boolean) x);
            stringFs = makeConstructorFieldSetter(null, pa, String.class, x -> (String) x);
            objectFs = (o, key, v) -> setProperty(pa, o, v);
        } else {
            name     = "None";
            nullFs   = null;
            longFs   = null;
            doubleFs = null;
            boolFs   = null;
            stringFs = null;
            objectFs = null;

        }
        return new CoercingPropertySetter(name + " property: " + pa.name(), nullFs, longFs, doubleFs, boolFs, stringFs, objectFs);
    }

    private static Class<?> getOneTypeArgClass(PropertyAccessor pa) {
        Type     genType  = pa.type();
        Class<?> argClass = null;
        if (genType instanceof ParameterizedType parGen && parGen.getActualTypeArguments().length == 1) {
            Type argType = parGen.getActualTypeArguments()[0];
            if (argType instanceof Class<?> argTypeClass) {
                argClass = argTypeClass;
            }
            if (argType instanceof ParameterizedType parGenType && parGenType.getRawType() instanceof Class<?> argTypeClass) {
                argClass = argTypeClass;
            }
        }
        return argClass;
    }

    private static <PRIM> PropertySetter makeConstructorFieldSetter(PropertySetter setter, PropertyAccessor pa, Class<PRIM> primClass, Function<Object, PRIM> convert) {
        return setter != null ? setter : Arrays.stream(pa.clazz().getConstructors())
                                               .filter(c -> c.getParameterCount() == 1 && (c.getParameterTypes()[0] == primClass || c.getParameterTypes()[0] == U.box(primClass)))
                                               .findFirst()
                                               .map(c -> (PropertySetter) (o, key, v) -> {
                                                   try {
                                                       setProperty(pa, o, c.newInstance(convert.apply(v)));
                                                       return o;
                                                   } catch (InstantiationException |
                                                            IllegalAccessException |
                                                            InvocationTargetException e) {
                                                       throw new IllegalArgumentException("can not make new instance of " + c.getDeclaringClass().getSimpleName() + " with arg (" + v + ")", e);
                                                   }
                                               })
                                               .orElse(null);
    }

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final String         name;
    private final PropertySetter nullPropertySetter;
    private final PropertySetter longPropertySetter;
    private final PropertySetter doublePropertySetter;
    private final PropertySetter boolPropertySetter;
    private final PropertySetter stringPropertySetter;
    private final PropertySetter objectPropertySetter;

    CoercingPropertySetter(String name, PropertySetter nullFs, PropertySetter longFs, PropertySetter doubleFs, PropertySetter boolFs, PropertySetter stringFs, PropertySetter objectFs) {
        this.name                 = name;
        this.nullPropertySetter   = nullFs;
        this.longPropertySetter   = longFs;
        this.doublePropertySetter = doubleFs;
        this.boolPropertySetter   = boolFs;
        this.stringPropertySetter = stringFs;
        this.objectPropertySetter = objectFs;
    }

    @Override
    public Object set(Object o, Object key, Object v) {
        try {
            Class<?> valueClass = v == null ? null : v.getClass();
            if (valueClass == null) {
                notNull(nullPropertySetter, o, key, null).set(o, key, null);
            } else if (valueClass == Long.class) {
                notNull(longPropertySetter, o, key, v).set(o, key, v);
            } else if (valueClass == Double.class) {
                notNull(doublePropertySetter, o, key, v).set(o, key, v);
            } else if (valueClass == Boolean.class) {
                notNull(boolPropertySetter, o, key, v).set(o, key, v);
            } else if (valueClass == String.class) {
                notNull(stringPropertySetter, o, key, v).set(o, key, v);
            } else {
                notNull(objectPropertySetter, o, key, v).set(o, key, v);
            }
            return o;
        } catch (Exception e) {
            throw new IllegalArgumentException("can't set " + o.getClass().getSimpleName() + "[" + key + "] = " + v, e);
        }
    }

    private static PropertySetter notNull(PropertySetter ss, Object o, Object key, Object v) {
        if (ss == null) {
            throw new IllegalArgumentException("field '" + key + "' of a '" + o.getClass().getSimpleName() + "' can not be set to " + (v == null ? "<null>" : "a '" + v.getClass().getSimpleName()) + "'");
        }
        return ss;
    }

    @Override
    public String toString() {
        return name;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static Object addToSetProperty(PropertyAccessor pa, Object o, Object v) {
        Set<Object> s = pa.get(o);
        if (s == null) {
            pa.set(o, new HashSet<>(List.of(v)));
        } else {
            s.add(v);
        }
        return o;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static Object addToListProperty(PropertyAccessor pa, Object o, Object v) {
        List<Object> l = pa.get(o);
        if (l == null) {
            pa.set(o, new HashSet<>(List.of(v)));
        } else {
            l.add(v);
        }
        return o;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static Object setProperty(PropertyAccessor pa, Object o, Object v) {
        pa.set(o, v);
        return o;
    }

    private static Object setProperty(PropertyAccessor pa, Object o, long v) {
        pa.set(o, v);
        return o;
    }

    private static Object setProperty(PropertyAccessor pa, Object o, int v) {
        pa.set(o, v);
        return o;
    }

    private static Object setProperty(PropertyAccessor pa, Object o, double v) {
        pa.set(o, v);
        return o;
    }

    private static Object setProperty(PropertyAccessor pa, Object o, String v) {
        pa.set(o, v);
        return o;
    }

    private static Object setProperty(PropertyAccessor pa, Object o, boolean v) {
        pa.set(o, v);
        return o;
    }

    private static Object setProperty(PropertyAccessor pa, Object o, short v) {
        pa.set(o, v);
        return o;
    }

    private static Object setProperty(PropertyAccessor pa, Object o, byte v) {
        pa.set(o, v);
        return o;
    }

    private static Object setProperty(PropertyAccessor pa, Object o, char v) {
        pa.set(o, v);
        return o;
    }

    private static Object setProperty(PropertyAccessor pa, Object o, float v) {
        pa.set(o, v);
        return o;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static Object setCollectionElement(Object o, Object v) {
        if (!(o instanceof Collection)) {
            throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName() + ")");
        }
        ((Collection<Object>) o).add(v);
        return o;
    }

    private static Object setCollectionElement(Object o, long v) {
        if (!(o instanceof Collection)) {
            throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName() + ")");
        }
        ((Collection<Long>) o).add(v);
        return o;
    }

    private static Object setCollectionElement(Object o, int v) {
        if (!(o instanceof Collection)) {
            throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName() + ")");
        }
        ((Collection<Integer>) o).add(v);
        return o;
    }

    private static Object setCollectionElement(Object o, double v) {
        if (!(o instanceof Collection)) {
            throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName() + ")");
        }
        ((Collection<Double>) o).add(v);
        return o;
    }

    private static Object setCollectionElement(Object o, String v) {
        if (!(o instanceof Collection)) {
            throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName() + ")");
        }
        ((Collection<String>) o).add(v);
        return o;
    }

    private static Object setCollectionElement(Object o, boolean v) {
        if (!(o instanceof Collection)) {
            throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName() + ")");
        }
        ((Collection<Boolean>) o).add(v);
        return o;
    }

    private static Object setCollectionElement(Object o, short v) {
        if (!(o instanceof Collection)) {
            throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName() + ")");
        }
        ((Collection<Short>) o).add(v);
        return o;
    }

    private static Object setCollectionElement(Object o, byte v) {
        if (!(o instanceof Collection)) {
            throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName() + ")");
        }
        ((Collection<Byte>) o).add(v);
        return o;
    }

    private static Object setCollectionElement(Object o, char v) {
        if (!(o instanceof Collection)) {
            throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName() + ")");
        }
        ((Collection<Character>) o).add(v);
        return o;
    }

    private static Object setCollectionElement(Object o, float v) {
        if (!(o instanceof Collection)) {
            throw new IllegalArgumentException("can't set an element of a non Collection object (" + o.getClass().getSimpleName() + ")");
        }
        ((Collection<Float>) o).add(v);
        return o;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static Object setMapElement(Object o, Object key, Object v) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName() + ")");
        }
        ((Map<Object, Object>) o).put(key, v);
        return o;
    }

    private static Object setMapElement(Object o, Object key, long v) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName() + ")");
        }
        ((Map<Object, Long>) o).put(key, v);
        return o;
    }

    private static Object setMapElement(Object o, Object key, int v) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName() + ")");
        }
        ((Map<Object, Integer>) o).put(key, v);
        return o;
    }

    private static Object setMapElement(Object o, Object key, double v) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName() + ")");
        }
        ((Map<Object, Double>) o).put(key, v);
        return o;
    }

    private static Object setMapElement(Object o, Object key, String v) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName() + ")");
        }
        ((Map<Object, String>) o).put(key, v);
        return o;
    }

    private static Object setMapElement(Object o, Object key, boolean v) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName() + ")");
        }
        ((Map<Object, Boolean>) o).put(key, v);
        return o;
    }

    private static Object setMapElement(Object o, Object key, short v) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName() + ")");
        }
        ((Map<Object, Short>) o).put(key, v);
        return o;
    }

    private static Object setMapElement(Object o, Object key, byte v) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName() + ")");
        }
        ((Map<Object, Byte>) o).put(key, v);
        return o;
    }

    private static Object setMapElement(Object o, Object key, char v) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName() + ")");
        }
        ((Map<Object, Character>) o).put(key, v);
        return o;
    }

    private static Object setMapElement(Object o, Object key, float v) {
        if (!(o instanceof Map)) {
            throw new IllegalArgumentException("can't set an element of a non Map object (" + o.getClass().getSimpleName() + ")");
        }
        ((Map<Object, Float>) o).put(key, v);
        return o;
    }
}
