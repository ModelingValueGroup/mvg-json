package org.modelingvalue.json;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class Config {
    public       boolean                       ignoreSFOs; // SFO = Single Field Object
    public       boolean                       includeClassNameInIntrospection;
    public final Map<Field, List<Annotation>>  extraFieldAnnotations  = new HashMap<>();
    public final Map<Method, List<Annotation>> extraMethodAnnotations = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Field f, Class<T> annotation) {
        List<Annotation> l = extraFieldAnnotations.get(f);
        return l == null ? f.getAnnotation(annotation) : l.stream().filter(a -> annotation.isAssignableFrom(a.getClass())).map(a -> (T) a).findFirst().orElseGet(() -> f.getAnnotation(annotation));
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Method m, Class<T> annotation) {
        List<Annotation> l = extraMethodAnnotations.get(m);
        return l == null ? m.getAnnotation(annotation) : l.stream().filter(a -> annotation.isAssignableFrom(a.getClass())).map(a -> (T) a).findFirst().orElseGet(() -> m.getAnnotation(annotation));
    }

    public void addJsonClassSelectorAnnotation(Method... ms) {
        JsonClassSelector a = new JsonClassSelector() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return JsonClassSelector.class;
            }
        };
        for (Method m : ms) {
            add(m, a);
        }
    }

    public void addJsonIdAnnotation(Field... fs) {
        JsonId a = new JsonId() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return JsonId.class;
            }
        };
        for (Field f : fs) {
            add(f, a);
        }
    }

    public void addJsonIdAnnotation(Method... ms) {
        JsonId a = new JsonId() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return JsonId.class;
            }
        };
        for (Method m : ms) {
            add(m, a);
        }
    }

    public void addJsonNameAnnotation(Field f, String name) {
        add(f, new JsonName() {
            @Override
            public String value() {
                return name;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return JsonName.class;
            }
        });
    }

    private void add(Field f, Annotation annotation) {
        extraFieldAnnotations.compute(f, (k, v) -> Stream.concat(v == null ? Stream.empty() : v.stream(), Stream.of(annotation)).collect(Collectors.toList()));
    }

    private void add(Method m, Annotation annotation) {
        extraMethodAnnotations.compute(m, (k, v) -> Stream.concat(v == null ? Stream.empty() : v.stream(), Stream.of(annotation)).collect(Collectors.toList()));
    }
}
