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

import java.util.Collection;
import java.util.Iterator;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.java.lang.TSystem;

@SubstitutesInClass(completeReplace = true)
public class TArrayList<T> {

    private static final int INITIAL_CAPACITY = 10;

    private Object[] data;
    private int currentLength;

    public TArrayList(Collection<T> aData) {
        this();
    }

    public TArrayList() {
        data = new Object[INITIAL_CAPACITY];
        currentLength =0;
    }

    public Object[] toArray() {
        Object[] theNewArray = new Object[currentLength];
        System.arraycopy(data, 0, theNewArray, 0, currentLength);
        return theNewArray;
    }

    public Object[] toArray(Object[] aTarget) {
        System.arraycopy(data, 0, aTarget, 0, currentLength);
        return aTarget;
    }

    public boolean add(T aObject) {
        currentLength++;
        if (currentLength >= data.length) {
            Object[] theNewData = new Object[data.length + INITIAL_CAPACITY];
            TSystem.arraycopy(data, 0, theNewData, 0, data.length);
            data = theNewData;
        }
        data[currentLength-1] = aObject;
        return true;
    }

    public T get(int aIndex) throws ArrayIndexOutOfBoundsException {
        if (aIndex >=currentLength || aIndex < 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return (T) data[aIndex];
    }

    public void clear() {
        currentLength = 0;
        data = new Object[INITIAL_CAPACITY];
    }

    public boolean contains(T aObject) {
        for (int i = 0; i<currentLength; i++) {
            Object theData = data[i];
            if (theData != null) {
                if (data[i].equals(aObject)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean remove(Object aObject) {
        for (int i = 0; i < currentLength ; i++) {
            Object theData = data[i];
            if (theData != null) {
                if (data[i].equals(aObject)) {
                    if (i!= currentLength -1) {
                        for (int k = i + 1; k < currentLength; k++) {
                            data[k - 1] = data[k];
                        }
                    }
                    currentLength--;
                    return true;
                }
            }
        }
        return false;
    }

    public int size() {
        return currentLength;
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private int index = 0;

            @Override
            public T next() {
                return (T) data[index++];
            }

            @Override
            public boolean hasNext() {
                return index < currentLength;
            }
        };
    }

    public boolean addAll(Collection<T> aOtherCollection) {
        boolean theChanged = false;
        for (T aValue : aOtherCollection) {
            if (add(aValue)) {
                theChanged = true;
            }
        }
        return theChanged;
    }

    public boolean removeAll(Collection<T> aOtherCollection) {
        boolean theChanged = false;
        for (T aValue : aOtherCollection) {
            if (remove(aValue)) {
                theChanged = true;
            }
        }
        return theChanged;
    }
}