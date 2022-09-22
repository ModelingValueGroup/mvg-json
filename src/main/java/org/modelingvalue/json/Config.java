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
