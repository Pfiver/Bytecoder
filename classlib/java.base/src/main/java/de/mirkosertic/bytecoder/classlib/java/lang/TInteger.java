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

import de.mirkosertic.bytecoder.api.NoExceptionCheck;

public class TInteger extends TNumber {

    private final int integerValue;

    @NoExceptionCheck
    public TInteger(int aIntegerValue) {
        integerValue = aIntegerValue;
    }

    @Override
    public int intValue() {
        return integerValue;
    }

    @Override
    public byte byteValue() {
        return (byte) integerValue;
    }

    @Override
    public short shortValue() {
        return (short) integerValue;
    }

    @Override
    public float floatValue() {
        return integerValue;
    }

    @Override
    public long longValue() {
        return integerValue;
    }

    @Override
    public double doubleValue() {
        return integerValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TInteger)) {
            return false;
        }

        TInteger theOtherInteger = (TInteger) o;

        return integerValue == theOtherInteger.integerValue;
    }

    @Override
    public int hashCode() {
        return integerValue;
    }

    @Override
    public String toString() {
        return toString(integerValue);
    }

    public static TInteger valueOf(int aValue) {
        return new TInteger(aValue);
    }

    public static TInteger valueOf(String aValue) {
        return new TInteger((int) TNumber.stringToLong(aValue));
    }

    public static int parseInt(String aString) {
        return (int) TNumber.stringToLong(aString);
    }

    public static String toString(int aValue) {
        TStringBuilder theBuffer = new TStringBuilder();
        theBuffer.append(aValue);
        return theBuffer.toString();
    }

    public static String toHexString(int aValue) {
        return TNumber.longToHex(aValue);
    }
}