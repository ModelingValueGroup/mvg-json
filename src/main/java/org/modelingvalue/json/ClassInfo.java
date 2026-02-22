//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

class ClassInfo {
    private final Class<?>               clazz;
    private final Config                 config;
    private final List<PropertyAccessor> properties    = new ArrayList<>();
    private final PropertyAccessor       idProperty;
    private final Set<Object>            seenBeforeSet = new HashSet<>();

    ClassInfo(Class<?> clazz, Config config) {
        this.clazz  = clazz;
        this.config = config;

        properties.addAll(PropertyAccessor.all(clazz, config)
                                          .values()
                                          .stream()
                                          .filter(PropertyAccessor::canGet)
                                          .sorted(Comparator.comparing(PropertyAccessor::name))
                                          .toList());
        idProperty = properties.stream().filter(pa -> pa.isId(config)).findFirst().orElse(null);
        if (idProperty != null) {
            properties.remove(idProperty);
            properties.add(0, idProperty);
        }
    }

    private boolean firstOccurrence(Object o) {
        if (idProperty == null) {
            return true;
        }
        if (!seenBeforeSet.contains(o)) {
            seenBeforeSet.add(o);
            return true;
        }
        return false;
    }

    public Iterator<Entry<Object, Object>> getIntrospectionIterator(Object o) {
        Stream<Entry<Object, Object>> entryStream;
        if (firstOccurrence(o)) {
            // first time, render the whole thing:
            entryStream = properties.stream().map(m -> new SimpleEntry<>(m.name(), m.get(o)));
            if (config.includeClassNameInIntrospection) {
                entryStream = Stream.concat(Stream.of(new SimpleEntry<>(U.CLASS_NAME_FIELD_NAME, clazz.getName())), entryStream);
            }
        } else {
            // this object was rendered before, only render the idProperty now:
            entryStream = Stream.of(new SimpleEntry<>(idProperty.name(), idProperty.get(o)));
        }
        return entryStream.iterator();
    }
}
