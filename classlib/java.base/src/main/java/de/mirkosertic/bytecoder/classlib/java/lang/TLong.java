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
package de.mirkosertic.bytecoder.classlib.java.lang;

public class TLong extends TNumber {

    private final long longValue;

    public TLong(long aLongValue) {
        longValue = aLongValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        TLong tLong = (TLong) o;

        if (longValue != tLong.longValue)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int) longValue;
    }

    @Override
    public int intValue() {
        return (int) longValue;
    }

    @Override
    public byte byteValue() {
        return (byte) longValue;
    }

    @Override
    public short shortValue() {
        return (short) longValue;
    }

    @Override
    public float floatValue() {
        return (float) longValue;
    }

    @Override
    public long longValue() {
        return longValue;
    }

    @Override
    public double doubleValue() {
        return longValue;
    }

    @Override
    public String toString() {
        return toString(longValue);
    }

    public static TLong valueOf(long aValue) {
        return new TLong(aValue);
    }

    public static TLong valueOf(String aValue) {
        return new TLong(TNumber.stringToLong(aValue));
    }

    public static long parseLong(String aString) {
        return TNumber.stringToLong(aString);
    }

    public static String toString(long aValue) {
        TStringBuilder theBuffer = new TStringBuilder();
        theBuffer.append(aValue);
        return theBuffer.toString();
    }

    public static String toHexString(long aValue) {
        return TNumber.longToHex(aValue);
    }
}