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

import java.util.Map;

import de.mirkosertic.bytecoder.classlib.java.io.TSerializable;
import de.mirkosertic.bytecoder.classlib.java.lang.TCloneable;

public class THashMap<K, V> extends TAbstractMap<K, V> implements TSerializable, TCloneable {

    private static class Bucket<K, V> {

        private final int hashCode;
        private final TArrayList<AbstractEntry<K, V>> values;

        public Bucket(int aHashCode) {
            hashCode = aHashCode;
            values = new TArrayList<>();
        }

        public boolean containsKey(K aKey) {
            for (int i=0;i<values.size();i++) {
                Entry<K, V> theEntry = values.get(i);
                if (theEntry.getKey().equals(aKey)) {
                    return true;
                }
            }
            return false;
        }

        public V put(K aKey, V aValue) {
            for (int i=0;i<values.size();i++) {
                AbstractEntry<K, V> theEntry = values.get(i);
                if (theEntry.key.equals(aKey)) {
                    V theOldValue = theEntry.value;
                    theEntry.value = aValue;
                    return theOldValue;
                }
            }
            AbstractEntry<K, V> theNewEntry = new AbstractEntry<>(aKey, aValue);
            values.add(theNewEntry);
            return null;
        }

        public V get(K aKey) {
            for (int i=0;i<values.size();i++) {
                AbstractEntry<K, V> theEntry = values.get(i);
                if (theEntry.key.equals(aKey)) {
                    return theEntry.value;
                }
            }
            return null;
        }

        public V remove(K aKey) {
            for (int i=0;i<values.size();i++) {
                AbstractEntry<K, V> theEntry = values.get(i);
                if (theEntry.key.equals(aKey)) {
                    V theOldValue = theEntry.value;
                    values.remove(theEntry);
                    return theOldValue;
                }
            }
            return null;
        }

        public boolean isEmpty() {
            return values.isEmpty();
        }
    }

    private final TArrayList<Bucket> buckets;

    public THashMap() {
        buckets = new TArrayList<>();
    }

    private Bucket<K, V> findByHashCode(int aHashCode) {
        for (int i=0;i<buckets.size();i++) {
            Bucket<K, V> theBuckets = buckets.get(i);
            if (theBuckets.hashCode == aHashCode) {
                return theBuckets;
            }
        }
        return null;
    }

    @Override
    public boolean containsKey(K aKey) {
        Bucket<K, V> theBucket = findByHashCode(aKey.hashCode());
        if (theBucket != null) {
            return theBucket.containsKey(aKey);
        }
        return false;
    }

    @Override
    public V put(K aKey, V aValue) {
        int theHashCode = aKey.hashCode();
        Bucket<K, V> theBucket = findByHashCode(theHashCode);
        if (theBucket == null) {
            theBucket = new Bucket<>(theHashCode);
            buckets.add(theBucket);
        }
        return theBucket.put(aKey, aValue);
    }

    @Override
    public V get(K aKey) {
        Bucket<K, V> theBucket = findByHashCode(aKey.hashCode());
        if (theBucket != null) {
            return theBucket.get(aKey);
        }
        return null;
    }

    @Override
    public V remove(K aKey) {
        Bucket<K, V> theBucket = findByHashCode(aKey.hashCode());
        if (theBucket != null) {
            V theOldValue = theBucket.remove(aKey);
            if (theBucket.isEmpty()) {
                buckets.remove(theBucket);
            }
            return theOldValue;
        }
        return null;
    }

    @Override
    public TCollection<V> values() {
        TSet<V> theResult = new THashSet<>();
        for (int i=0;i<buckets.size();i++) {
            TArrayList<Map.Entry<K,V>> theEntries = buckets.get(i).values;
            for (int k=0;k<theEntries.size();k++) {
                theResult.add(theEntries.get(k).getValue());
            }
        }
        return theResult;
    }

    @Override
    public TSet<TMap.Entry<K, V>> entrySet() {
        TSet<TMap.Entry<K,V>> theResult = new THashSet<>();
        for (int i=0;i<buckets.size();i++) {
            TArrayList<AbstractEntry<K, V>> theValues = buckets.get(i).values;
            for (int j=0;j<theValues.size();j++) {
                theResult.add(theValues.get(j));
            }
        }
        return theResult;
    }

    @Override
    public TSet<K> keySet() {
        TSet<K> theResult = new THashSet<>();
        for (int i=0;i<buckets.size();i++) {
            TArrayList<AbstractEntry<K,V>> theEntries = buckets.get(i).values;
            for (int k=0;k<theEntries.size();k++) {
                theResult.add(theEntries.get(k).key);
            }
        }
        return theResult;
    }
}