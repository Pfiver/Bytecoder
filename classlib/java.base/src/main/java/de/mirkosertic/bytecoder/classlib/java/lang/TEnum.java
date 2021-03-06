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
import de.mirkosertic.bytecoder.classlib.java.io.TSerializable;

import java.util.Objects;

public class TEnum extends TObject implements TSerializable {

    private final TString name;
    private final int ordinalNumber;

    @NoExceptionCheck
    protected TEnum(TString aName, int aOrdinalNumber) {
        name = aName;
        ordinalNumber = aOrdinalNumber;
    }

    public int ordinal() {
        return ordinalNumber;
    }

    public TString name() {
        return name;
    }

    public static Enum valueOf(Class<Enum> aClass, String aValue) {
        for (Enum theEnum : aClass.getEnumConstants()) {
            if (Objects.equals(theEnum.name(), aValue)) {
                return theEnum;
            }
        }
        throw new IllegalArgumentException();
    }
}
