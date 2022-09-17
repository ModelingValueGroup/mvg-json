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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class ToJson {
    private static final String            NULL_STRING                 = "null";
    private static final Predicate<Field>  FIELD_INTROSPECTION_FILTER  = f -> !f.isSynthetic()//
                                                                              && !f.isEnumConstant()//
                                                                              && !Modifier.isStatic(f.getModifiers())//
                                                                              && !Modifier.isVolatile(f.getModifiers())//
                                                                              && !Modifier.isNative(f.getModifiers())//
                                                                              && !Modifier.isTransient(f.getModifiers())//
                                                                              && (Modifier.isPublic(f.getModifiers()) || !f.getDeclaringClass().getPackage().getName().startsWith("java."));
    private static final Predicate<Method> METHOD_INTROSPECTION_FILTER = m -> !m.isSynthetic()//
                                                                              && m.getParameterCount() == 0//
                                                                              && m.getReturnType() != Void.class//
                                                                              && !m.isDefault()//
                                                                              && !Modifier.isStatic(m.getModifiers())//
                                                                              && !Modifier.isVolatile(m.getModifiers())//
                                                                              && !Modifier.isNative(m.getModifiers())//
                                                                              && !Modifier.isTransient(m.getModifiers())//
                                                                              && (Modifier.isPublic(m.getModifiers()) || !m.getDeclaringClass().getPackage().getName().startsWith("java."))//
                                                                              && m.getName().matches("^(get|is)[A-Z].*");

    public static String toJson(Object o) {
        return new ToJson(o).render();
    }

    private final Object                   root;
    private final StringBuilder            b            = new StringBuilder();
    private       int                      level;
    private       int                      index;
    private       boolean                  ignoreSFOs; // SFO = Single Field Object
    private       boolean                  includeClassNameInIntrospection;
    private       Function<Object, String> idFunction;
    private final Map<Class<?>, ClassInfo> classInfoMap = new HashMap<>();
    private       Map<Object, String>      seenBeforeMap;

    private class ClassInfo {
        private final Class<?>            clazz;
        private       List<Field>         fields;
        private final Map<Field, String>  field2name  = new HashMap<>();
        private       List<Method>        methods;
        private final Map<Method, String> method2name = new HashMap<>();

        private ClassInfo(Class<?> clazz) {
            this.clazz = clazz;
        }

        private Map<String, Object> getIntrospectionMap(Object o) {
            Map<String, Object> map = new HashMap<>();
            if (includeClassNameInIntrospection) {
                map.put("~className", clazz.getName());
            }
            String id = seenBefore(o);
            if (id != null) {
                map.put("~id", id);
            } else {
                putInIntrospectionMap(map, o);
            }
            return map;
        }

        private void putInIntrospectionMap(Map<String, Object> map, Object o) {
            getFields().forEach(f -> {
                try {
                    map.put(getName(f), f.get(o));
                } catch (IllegalAccessException e) {
                    // ignore, just leave out this element
                }
            });
            getMethods().forEach(m -> {
                try {
                    map.put(getName(m), m.invoke(o));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    // ignore, just leave out this element
                }
            });
        }

        private List<Field> getFields() {
            if (fields == null) {
                fields = new ArrayList<>();
                for (Class<?> c = clazz; c != Object.class; c = c.getSuperclass()) {
                    fields.addAll(Arrays.asList(c.getDeclaredFields()));
                }
                fields.removeIf(FIELD_INTROSPECTION_FILTER.negate());
                fields.forEach(f -> f.setAccessible(true));
                Collections.reverse(fields);
            }
            return fields;
        }

        private List<Method> getMethods() {
            if (methods == null) {
                methods = new ArrayList<>();
                for (Class<?> c = clazz; c != Object.class; c = c.getSuperclass()) {
                    methods.addAll(Arrays.asList(c.getDeclaredMethods()));
                }
                methods.removeIf(METHOD_INTROSPECTION_FILTER.negate());
                methods.forEach(m -> m.setAccessible(true));
            }
            return methods;
        }

        private String getName(Field f) {
            return field2name.computeIfAbsent(f, ff -> {
                JsonName nameAnnotation = f.getAnnotation(JsonName.class);
                return nameAnnotation == null ? f.getName() : nameAnnotation.value();
            });
        }

        private String getName(Method m) {
            return method2name.computeIfAbsent(m, mm -> m.getName().replaceAll("^(get|is)([A-Z]).*", "$2").toLowerCase() + m.getName().replaceAll("^(get|is)[A-Z]", ""));
        }
    }

    protected ToJson(Object o) {
        this.root = o;
    }

    @SuppressWarnings("unused")
    public void setIgnoreSFOs(boolean b) {
        ignoreSFOs = b;
    }

    @SuppressWarnings("unused")
    public void setIncludeClassNameInIntrospection(boolean b) {
        includeClassNameInIntrospection = b;
    }

    public void setIdFunction(Function<Object, String> idFunction) {
        this.idFunction = idFunction;
        seenBeforeMap   = idFunction == null ? null : seenBeforeMap == null ? new HashMap<>() : seenBeforeMap;
    }

    @SuppressWarnings("unused")
    public String render() {
        b.setLength(0);
        level = 0;
        index = 0;
        jsonFromAny(root);
        return b.toString();
    }

    @SuppressWarnings("unused")
    public int getLevel() {
        return level;
    }

    @SuppressWarnings("unused")
    public int getIndex() {
        return index;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    protected Object filter(Object o) {
        return o;
    }

    protected boolean isMapType(Object o) {
        return o instanceof Map;
    }

    @SuppressWarnings("unchecked")
    protected Iterator<Entry<Object, Object>> getMapIterator(Object o) {
        //noinspection unchecked
        return ((Map<Object, Object>) o).entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString())).iterator();
    }

    protected boolean isIterableType(Object o) {
        return o instanceof Iterable;
    }

    @SuppressWarnings("unchecked")
    protected Iterator<Object> getArrayIterator(Object o) {
        //noinspection unchecked
        return ((Iterable<Object>) o).iterator();
    }

    protected Map<String, Object> getIntrospectionMap(Object o) {
        Class<?> clazz = o.getClass();
        return classInfoMap.computeIfAbsent(clazz, t -> new ClassInfo(clazz)).getIntrospectionMap(o);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    protected void jsonFromAny(Object o) {
        o = filter(o);
        o = replaceSFO(o);
        if (o == null) {
            b.append(NULL_STRING);

        } else if (isMapType(o)) {
            jsonFromMap(o);
        } else if (isIterableType(o)) {
            jsonFromIterable(o);

        } else if (o instanceof String) {
            jsonFromString((String) o);
        } else if (o instanceof Byte) {
            b.append((byte) o);
        } else if (o instanceof Short) {
            b.append((short) o);
        } else if (o instanceof Integer) {
            b.append((int) o);
        } else if (o instanceof Long) {
            b.append((long) o);
        } else if (o instanceof Character) {
            jsonFromCharacter((Character) o);
        } else if (o instanceof Float) {
            b.append((float) o);
        } else if (o instanceof Double) {
            b.append((double) o);
        } else if (o instanceof Boolean) {
            b.append((boolean) o);

        } else if (o instanceof String[]) {
            jsonFromStringArray((String[]) o);
        } else if (o instanceof byte[]) {
            jsonFromByteArray((byte[]) o);
        } else if (o instanceof short[]) {
            jsonFromShortArray((short[]) o);
        } else if (o instanceof int[]) {
            jsonFromIntArray((int[]) o);
        } else if (o instanceof long[]) {
            jsonFromLongArray((long[]) o);
        } else if (o instanceof char[]) {
            jsonFromCharArray((char[]) o);
        } else if (o instanceof float[]) {
            jsonFromFloatArray((float[]) o);
        } else if (o instanceof double[]) {
            jsonFromDoubleArray((double[]) o);
        } else if (o instanceof boolean[]) {
            jsonFromBooleanArray((boolean[]) o);
        } else if (o instanceof Object[]) {
            jsonFromObjectArray((Object[]) o);
        } else {
            jsonFromMap(getIntrospectionMap(o));
        }
    }

    private Object replaceSFO(Object o) {
        Class<?> clazz;
        if (!ignoreSFOs//
            && o != null//
            && !(clazz = o.getClass()).isPrimitive()//
            && clazz.getSuperclass() == Object.class//
            && GenericsUtil.unbox(clazz) == clazz//
        ) {
            Field[]          fields = clazz.getDeclaredFields();
            Constructor<?>[] constructors;
            if (fields.length == 1//
                && (constructors = clazz.getDeclaredConstructors()).length == 1//
                && constructors[0].getParameterCount() == 1//
                && constructors[0].getParameterTypes()[0] == fields[0].getType()//
            ) {
                try {
                    fields[0].setAccessible(true);
                    o = fields[0].get(o);
                } catch (IllegalAccessException e) {
                    // skip the SFO
                }
            }
        }
        return o;
    }

    protected void jsonFromMap(Object o) {
        b.append('{');
        Object id = seenBefore(o);
        if (id != null) {
            b.append("\"$id\":\"").append(id);
        } else {
            Iterator<Entry<Object, Object>> it  = getMapIterator(o);
            String                          sep = "";
            level++;
            int savedIndex = index;
            index = 0;
            while (it.hasNext()) {
                Entry<Object, Object> e = it.next();
                b.append(sep);
                sep = ",";
                jsonFromString(stringFromKey(e.getKey()));
                b.append(":");
                jsonFromAny(e.getValue());
                index++;
            }
            index = savedIndex;
            level--;
        }
        b.append('}');
    }

    private String seenBefore(Object o) {
        if (seenBeforeMap != null) {
            if (seenBeforeMap.containsKey(o)) {
                return seenBeforeMap.get(o);
            } else {
                seenBeforeMap.put(o, idFunction.apply(o));
            }
        }
        return null;
    }

    protected String stringFromKey(Object key) {
        return Objects.requireNonNull(key, "can not make json: a map contains a null key").toString();
    }

    protected void jsonFromIterable(Object o) {
        b.append('[');
        Iterator<Object> it  = getArrayIterator(o);
        String           sep = "";
        level++;
        int savedIndex = index;
        index = 0;
        while (it.hasNext()) {
            b.append(sep);
            sep = ",";
            jsonFromAny(it.next());
            index++;
        }
        index = savedIndex;
        level--;
        b.append(']');
    }

    protected void jsonFromByteArray(byte[] o) {
        b.append('[');
        String sep = "";
        level++;
        int savedIndex = index;
        index = 0;
        for (byte oo : o) {
            b.append(sep);
            sep = ",";
            b.append(oo);
            index++;
        }
        index = savedIndex;
        level--;
        b.append(']');
    }

    protected void jsonFromBooleanArray(boolean[] o) {
        b.append('[');
        String sep = "";
        level++;
        int savedIndex = index;
        index = 0;
        for (boolean oo : o) {
            b.append(sep);
            sep = ",";
            b.append(oo);
            index++;
        }
        index = savedIndex;
        level--;
        b.append(']');
    }

    protected void jsonFromObjectArray(Object[] o) {
        b.append('[');
        String sep = "";
        level++;
        int savedIndex = index;
        index = 0;
        for (Object oo : o) {
            b.append(sep);
            sep = ",";
            jsonFromAny(oo);
            index++;
        }
        index = savedIndex;
        level--;
        b.append(']');
    }

    protected void jsonFromDoubleArray(double[] o) {
        b.append('[');
        String sep = "";
        level++;
        int savedIndex = index;
        index = 0;
        for (double oo : o) {
            b.append(sep);
            sep = ",";
            b.append(oo);
            index++;
        }
        index = savedIndex;
        level--;
        b.append(']');
    }

    protected void jsonFromFloatArray(float[] o) {
        b.append('[');
        String sep = "";
        level++;
        int savedIndex = index;
        index = 0;
        for (float oo : o) {
            b.append(sep);
            sep = ",";
            b.append(oo);
            index++;
        }
        index = savedIndex;
        level--;
        b.append(']');
    }

    protected void jsonFromCharArray(char[] o) {
        b.append('[');
        String sep = "";
        level++;
        int savedIndex = index;
        index = 0;
        for (char oo : o) {
            b.append(sep);
            sep = ",";
            jsonFromCharacter(oo);
            index++;
        }
        index = savedIndex;
        level--;
        b.append(']');
    }

    protected void jsonFromLongArray(long[] o) {
        b.append('[');
        String sep = "";
        level++;
        int savedIndex = index;
        index = 0;
        for (long oo : o) {
            b.append(sep);
            sep = ",";
            b.append(oo);
            index++;
        }
        index = savedIndex;
        level--;
        b.append(']');
    }

    protected void jsonFromShortArray(short[] o) {
        b.append('[');
        String sep = "";
        level++;
        int savedIndex = index;
        index = 0;
        for (short oo : o) {
            b.append(sep);
            sep = ",";
            b.append(oo);
            index++;
        }
        index = savedIndex;
        level--;
        b.append(']');
    }

    protected void jsonFromIntArray(int[] o) {
        b.append('[');
        String sep = "";
        level++;
        int savedIndex = index;
        index = 0;
        for (int oo : o) {
            b.append(sep);
            sep = ",";
            b.append(oo);
            index++;
        }
        index = savedIndex;
        level--;
        b.append(']');
    }

    protected void jsonFromStringArray(String[] o) {
        b.append('[');
        String sep = "";
        level++;
        int savedIndex = index;
        index = 0;
        for (String oo : o) {
            b.append(sep);
            sep = ",";
            jsonFromString(oo);
            index++;
        }
        index = savedIndex;
        level--;
        b.append(']');
    }

    protected void jsonFromCharacter(char o) {
        b.append('"');
        appendStringCharacter(o);
        b.append('"');
    }

    protected void jsonFromString(String o) {
        if (o == null) {
            b.append("\"null\"");
        } else {
            b.append('"');
            final int length = o.length();
            for (int i = 0; i < length; i++) {
                appendStringCharacter(o.charAt(i));
            }
            b.append('"');
        }
    }

    private void appendStringCharacter(char ch) {
        switch (ch) {
        case '"':
            b.append("\\\"");
            break;
        // JSON allows escaping '/' but it does not require it to be escaped:
        //        case '/':
        //            b.append("\\/");
        //            break;
        case '\\':
            b.append("\\\\");
            break;
        case '\b':
            b.append("\\b");
            break;
        case '\f':
            b.append("\\f");
            break;
        case '\n':
            b.append("\\n");
            break;
        case '\r':
            b.append("\\r");
            break;
        case '\t':
            b.append("\\t");
            break;
        default:
            // see: https://www.unicode.org
            if (ch <= '\u001F' || ('\u007F' <= ch && ch <= '\u009F') || ('\u2000' <= ch && ch <= '\u20FF')) {
                String ss = Integer.toHexString(ch);
                b.append("\\u");
                b.append("0".repeat(4 - ss.length()));
                b.append(ss.toUpperCase());
            } else {
                b.append(ch);
            }
        }
    }
}
