/*
 * Copyright 2017 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.bytecoder.classlib.java.util;

public interface TMap<K, V> {

    interface Entry<K, V> {

        K getKey();

        V getValue();
    }

    boolean containsKey(K aKey);

    V put(K aKey, V aValue);

    V get(K aKey);

    V remove(K aKey);

    TCollection<V> values();

    TSet<K> keySet();

    TSet<Entry<K, V>> entrySet();
}
