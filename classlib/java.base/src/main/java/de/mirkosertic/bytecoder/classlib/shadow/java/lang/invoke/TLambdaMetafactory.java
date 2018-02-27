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
package de.mirkosertic.bytecoder.classlib.shadow.java.lang.invoke;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;
import de.mirkosertic.bytecoder.classlib.VM;

@SubstitutesInClass(completeReplace = true)
public class TLambdaMetafactory {

    public static CallSite metafactory(MethodHandles.Lookup aCaller,
                                        String aName,
                                        MethodType aInvokedType,
                                        MethodType aSamMethodType,
                                        MethodHandle aImplMethod,
                                        MethodType aInstantiatedMethodType) throws Throwable {

        VM.RuntimeGeneratedType theType = new VM.RuntimeGeneratedType(aInvokedType, aImplMethod);

        return new VM.ImplementingCallsite(null) {
            @Override
            public Object invokeExact(Object... args) throws Throwable {
                return theType;
            }
        };
    }
}