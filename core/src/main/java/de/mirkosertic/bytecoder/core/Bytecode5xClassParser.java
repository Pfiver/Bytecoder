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
package de.mirkosertic.bytecoder.core;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.mirkosertic.bytecoder.api.IsObject;

// https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7
public class Bytecode5xClassParser implements BytecodeClassParser {

    private static final int CONSTANT_Class = 7;
    private static final int CONSTANT_Fieldref = 9;
    private static final int CONSTANT_Methodref = 10;
    private static final int CONSTANT_InterfaceMethodref= 11;
    private static final int CONSTANT_String = 8;
    private static final int CONSTANT_Integer = 3;
    private static final int CONSTANT_Float = 4;
    private static final int CONSTANT_Long = 5;
    private static final int CONSTANT_Double = 6;
    private static final int CONSTANT_NameAndType = 12;
    private static final int CONSTANT_Utf8 = 1;
    private static final int CONSTANT_MethodHandle = 15;
    private static final int CONSTANT_MethodType = 16;
    private static final int CONSTANT_InvokeDynamic = 18;

    private final BytecodeProgramParser programmParser;
    private final BytecodeSignatureParser signatureParser;
    private final BytecodeReplacer bytecodeReplacer;

    public Bytecode5xClassParser(BytecodeProgramParser aParser,
            BytecodeSignatureParser aSignatureParser,
            BytecodeReplacer aReplacer) {
        programmParser = aParser;
        signatureParser = aSignatureParser;
        bytecodeReplacer = aReplacer;
    }

    @Override
    public BytecodeClass parseBody(DataInput dis) throws IOException {

        BytecodeConstantPool theConstantPool = parseConstantPool(dis);

        BytecodeAccessFlags theAccessFlags = parseAccessFlags(dis);
        BytecodeClassinfoConstant theThisClass = parseThisClass(dis, theConstantPool);
        BytecodeClassinfoConstant theSuperClass = parseSuperClass(dis, theConstantPool);

        BytecodeInterface[] theInterfaces = parseInterfaces(dis, theConstantPool);
        BytecodeField[] theFields = parseFields(dis, theConstantPool);
        BytecodeMethod[] theMethods = parseMethods(dis, theConstantPool);

        BytecodeAttributeInfo[] theClassAttributes = parseAttributes(dis, theConstantPool);

        BytecodeAttributes theAttributes = new BytecodeAttributes(theClassAttributes);
        if (theAttributes.getAnnotationByType(IsObject.class.getName()) != null) {
            theSuperClass = BytecodeClassinfoConstant.OBJECT_CLASS;
        }

        return new BytecodeClass(theConstantPool,
                theAccessFlags,
                theThisClass,
                theSuperClass,
                theInterfaces,
                theFields,
                bytecodeReplacer.replace(theThisClass, theMethods),
                theClassAttributes);
    }

    private BytecodeConstantPool parseConstantPool(DataInput aDis) throws IOException {
        BytecodeConstantPool theResult = new BytecodeConstantPool();
        int theConstantPoolCount = aDis.readUnsignedShort();
        for (int i=1;i<theConstantPoolCount;i++) {
            int theTag = aDis.readUnsignedByte();
            switch (theTag) {
            case CONSTANT_Class:
                parseConstantPool_CONSTANT_Class(aDis, theResult);
                break;
            case CONSTANT_Fieldref:
                parseConstantPool_CONSTANT_Fieldref(aDis, theResult);
                break;
            case CONSTANT_Methodref:
                parseConstantPool_CONSTANT_Methodref(aDis, theResult);
                break;
            case CONSTANT_InterfaceMethodref:
                parseConstantPool_CONSTANT_InterfaceMethodref(aDis, theResult);
                break;
            case CONSTANT_String:
                parseConstantPool_CONSTANT_String(aDis, theResult);
                break;
            case CONSTANT_Integer:
                parseConstantPool_CONSTANT_Integer(aDis, theResult);
                break;
            case CONSTANT_Float:
                parseConstantPool_CONSTANT_Float(aDis, theResult);
                break;
            case CONSTANT_Long:
                parseConstantPool_CONSTANT_Long(aDis, theResult);
                theResult.registerConstant(new BytecodeUnusedConstant());
                i++;
                break;
            case CONSTANT_Double:
                parseConstantPool_CONSTANT_Double(aDis, theResult);
                theResult.registerConstant(new BytecodeUnusedConstant());
                i++;
                break;
            case CONSTANT_NameAndType:
                parseConstantPool_CONSTANT_NameAndType(aDis, theResult);
                break;
            case CONSTANT_Utf8:
                parseConstantPool_CONSTANT_Utf8(aDis, theResult);
                break;
            case CONSTANT_MethodHandle:
                parseConstantPool_CONSTANT_MethodHandle(aDis, theResult);
                break;
            case CONSTANT_MethodType:
                parseConstantPool_CONSTANT_MethodType(aDis, theResult);
                break;
            case CONSTANT_InvokeDynamic:
                parseConstantPool_CONSTANT_InvokeDynamic(aDis, theResult);
                break;
            default:
                throw new IllegalStateException("Unknown constant pool tag : " + theTag + " for index " + i + " of " + theConstantPoolCount);
            }
        }

        return theResult;
    }

    private void parseConstantPool_CONSTANT_Class(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theNameIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeClassinfoConstant(theNameIndex, aConstantPool));
    }

    private void parseConstantPool_CONSTANT_Fieldref(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theClassIndex = aDis.readUnsignedShort();
        int theNameAndTypeIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeFieldRefConstant(new BytecodeClassIndex(theClassIndex, aConstantPool), new BytecodeNameAndTypeIndex(theNameAndTypeIndex, aConstantPool)));
    }

    private void parseConstantPool_CONSTANT_Methodref(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theClassIndex = aDis.readUnsignedShort();
        int theNameAndTypeIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeMethodRefConstant(new BytecodeClassIndex(theClassIndex, aConstantPool), new BytecodeNameAndTypeIndex(theNameAndTypeIndex, aConstantPool)));
    }

    private void parseConstantPool_CONSTANT_InterfaceMethodref(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theClassIndex = aDis.readUnsignedShort();
        int theNameAndTypeIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeInterfaceRefConstant(new BytecodeClassIndex(theClassIndex, aConstantPool), new BytecodeNameAndTypeIndex(theNameAndTypeIndex, aConstantPool)));
    }

    private void parseConstantPool_CONSTANT_String(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theStringIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeStringConstant(new BytecodeStringIndex(theStringIndex), aConstantPool));
    }

    private void parseConstantPool_CONSTANT_Integer(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theBytes = aDis.readInt();
        aConstantPool.registerConstant(new BytecodeIntegerConstant(theBytes));
    }

    private void parseConstantPool_CONSTANT_Float(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        float theFloat = aDis.readFloat();
        aConstantPool.registerConstant(new BytecodeFloatConstant(theFloat));
    }

    private void parseConstantPool_CONSTANT_Long(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        long theLowBytes = aDis.readInt() & 0xFFFFFFFFL;
        long theHighBytes = aDis.readInt() & 0xFFFFFFFFL;
        aConstantPool.registerConstant(new BytecodeLongConstant(theHighBytes, theLowBytes));
    }

    private void parseConstantPool_CONSTANT_Double(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        double theDouble = aDis.readDouble();
        aConstantPool.registerConstant(new BytecodeDoubleConstant(theDouble));
    }

    private void parseConstantPool_CONSTANT_NameAndType(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theNameIndex = aDis.readUnsignedShort();
        int theDescriptorIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeNameAndTypeConstant(new BytecodeNameIndex(theNameIndex, aConstantPool), new BytecodeDescriptorIndex(theDescriptorIndex, aConstantPool, signatureParser)));
    }

    private void parseConstantPool_CONSTANT_Utf8(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theLength = aDis.readUnsignedShort();
        byte[] theData = new byte[theLength];
        aDis.readFully(theData);
        aConstantPool.registerConstant(new BytecodeUtf8Constant(new String(theData, "UTF-8")));
    }

    private void parseConstantPool_CONSTANT_MethodHandle(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theReferenceKind = aDis.readUnsignedByte();
        int theReferenceIndex = aDis.readUnsignedShort();
        switch (theReferenceKind) {
        case 1:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_getField, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 2:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_getStatic, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 3:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_putField, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 4:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_putStatic, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 5:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_invokeVirtual, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 6:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_invokeStatic, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 7:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_invokeSpecial, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 8:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_newInvokeSpecial, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        case 9:
            aConstantPool.registerConstant(new BytecodeMethodHandleConstant(BytecodeReferenceKind.REF_invokeInterface, new BytecodeReferenceIndex(theReferenceIndex, aConstantPool)));
            break;
        default:
            throw new IllegalStateException("Unknown reference kind : " + theReferenceKind);
        }
    }

    private void parseConstantPool_CONSTANT_MethodType(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theDescriptorIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeMethodTypeConstant(new BytecodeDescriptorIndex(theDescriptorIndex, aConstantPool, signatureParser)));
    }

    private void parseConstantPool_CONSTANT_InvokeDynamic(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theBootstrapMethodAttrIndex = aDis.readUnsignedShort();
        int theNameAndTypeIndex = aDis.readUnsignedShort();
        aConstantPool.registerConstant(new BytecodeInvokeDynamicConstant(new BytecodeMethodAttributeIndex(theBootstrapMethodAttrIndex),
                new BytecodeNameAndTypeIndex(theNameAndTypeIndex, aConstantPool)));
    }

    private BytecodeAccessFlags parseAccessFlags(DataInput aDis) throws IOException {
        int theAccessFlags = aDis.readUnsignedShort();
        return new BytecodeAccessFlags(theAccessFlags);
    }

    private BytecodeClassinfoConstant parseThisClass(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theThisClass = aDis.readUnsignedShort();
        BytecodeConstant theConstant = aConstantPool.constantByIndex(theThisClass - 1);
        if (!(theConstant instanceof BytecodeClassinfoConstant)) {
            throw new IllegalStateException("Invalid this constant reference : got type " + theConstant.getClass().getName());
        }
        return (BytecodeClassinfoConstant) theConstant;
    }

    private BytecodeClassinfoConstant parseSuperClass(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theSuperClass = aDis.readUnsignedShort();
        if (theSuperClass == 0) {
            return BytecodeClassinfoConstant.OBJECT_CLASS;
        }
        BytecodeConstant theConstant = aConstantPool.constantByIndex(theSuperClass - 1);
        if (!(theConstant instanceof BytecodeClassinfoConstant)) {
            throw new IllegalStateException("Invalid super_class constant reference : got type " + theConstant.getClass().getName());
        }
        return (BytecodeClassinfoConstant) theConstant;
    }

    private BytecodeInterface[] parseInterfaces(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        List<BytecodeInterface> theInterfaces = new ArrayList<>();
        int theInterfaceCount = aDis.readUnsignedShort();
        for (int i=0;i<theInterfaceCount;i++) {
            int theNameIndex = aDis.readUnsignedShort();
            BytecodeConstant theConstant = aConstantPool.constantByIndex(theNameIndex - 1);
            if (!(theConstant instanceof BytecodeClassinfoConstant)) {
                throw new IllegalStateException("Invalid constant reference : got type " + theConstant.getClass().getName());
            }
            theInterfaces.add(new BytecodeInterface((BytecodeClassinfoConstant) theConstant));
        }
        return theInterfaces.toArray(new BytecodeInterface[theInterfaces.size()]);
    }

    private BytecodeBootstrapMethodsAttributeInfo parseBootstrapAttribute(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theNumMethods = aDis.readUnsignedShort();
        List<BytecodeBootstrapMethod> theMethods = new ArrayList<>();
        for (int i=0;i<theNumMethods;i++) {
            int theMethodRef = aDis.readUnsignedShort();
            int theNumArguments = aDis.readUnsignedShort();
            int[] theArguments = new int[theNumArguments];
            for (int j=0;j<theNumArguments;j++) {
                theArguments[j] = aDis.readUnsignedShort();
            }
            theMethods.add(new BytecodeBootstrapMethod(theMethodRef, theArguments, aConstantPool));
        }

        return new BytecodeBootstrapMethodsAttributeInfo(theMethods.toArray(new BytecodeBootstrapMethod[theMethods.size()]));
    }

    private BytecodeLocalVariableTableAttributeInfo parseLocalVariableTableAttribute(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theNumTableEntries = aDis.readUnsignedShort();
        List<BytecodeLocalVariableTableEntry> theEntries = new ArrayList<>();
        for (int i=0;i<theNumTableEntries;i++) {
            int theStartPC = aDis.readUnsignedShort();
            int theLength = aDis.readUnsignedShort();
            int theNameIndex = aDis.readUnsignedShort();
            int theDescriptorIndex = aDis.readUnsignedShort();
            int theIndex = aDis.readUnsignedShort();

            BytecodeTypeRef theTypeRef = signatureParser.toFieldType(((BytecodeUtf8Constant) aConstantPool.constantByIndex(theDescriptorIndex - 1)));

            theEntries.add(new BytecodeLocalVariableTableEntry(theStartPC, theLength, theNameIndex, theTypeRef, theIndex));
        }

        return new BytecodeLocalVariableTableAttributeInfo(aConstantPool, theEntries.toArray(new BytecodeLocalVariableTableEntry[theEntries.size()]));
    }

    private BytecodeAnnotation.ElementValue readAnnotationElementValueFrom(DataInput aDis, BytecodeConstantPool aConstantPool)
            throws IOException {
        char theTag = (char) aDis.readUnsignedByte();
        switch (theTag) {
        case 's': {
            int theConstValueIndex = aDis.readUnsignedShort();
            return new BytecodeAnnotation.StringElementValue(theConstValueIndex, aConstantPool);
        }
        case 'I': {
            int theConstValueIndex = aDis.readUnsignedShort();
            return new BytecodeAnnotation.IntegerElementValue(theConstValueIndex, aConstantPool);
        }
        case 'c': {
            int theClassInfoIndex = aDis.readUnsignedShort();
            return new BytecodeAnnotation.ClassElementValue(theClassInfoIndex, aConstantPool, signatureParser);
        }
        case 'Z': {
            int theClassInfoIndex = aDis.readUnsignedShort();
            return new BytecodeAnnotation.BooleanElementValue(theClassInfoIndex, aConstantPool);
        }
        case 'e': {
            int theTypeNameIndex = aDis.readUnsignedShort();
            int theConstNameIndex = aDis.readUnsignedShort();
            return new BytecodeAnnotation.EnumElementValue(aConstantPool,
                    theTypeNameIndex, theConstNameIndex);
        }
        case '[': {
            int theLength = aDis.readUnsignedShort();
            BytecodeAnnotation.ElementValue[] theValues = new BytecodeAnnotation.ElementValue[theLength];
            for (int i=0;i<theLength;i++) {
                theValues[i] = readAnnotationElementValueFrom(aDis, aConstantPool);
            }
            return new BytecodeAnnotation.ArrayElementValue(aConstantPool, theValues);
        }
        }
        throw new IllegalArgumentException("Not supported annotation value type : " + theTag);
    }

    private BytecodeAnnotationAttributeInfo parseAnnotationAttribute(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        int theAnnotationCount = aDis.readUnsignedShort();
        List<BytecodeAnnotation> theAnnotations = new ArrayList<>();
        for (int i=0;i<theAnnotationCount;i++) {
            int theTypeIndex = aDis.readUnsignedShort();
            int theNumElementValuePairs = aDis.readUnsignedShort();

            List<BytecodeAnnotation.ElementValuePair> theElementValuePairs = new ArrayList<>();
            for (int j=0;j<theNumElementValuePairs;j++) {
                int theElementNameIndex = aDis.readUnsignedShort();
                BytecodeAnnotation.ElementValue theAnnotationValue =  readAnnotationElementValueFrom(aDis, aConstantPool);
                theElementValuePairs.add(new BytecodeAnnotation.ElementValuePair(theElementNameIndex,
                        theAnnotationValue,
                        aConstantPool));
            }

            theAnnotations.add(new BytecodeAnnotation(theTypeIndex, theElementValuePairs.toArray(new BytecodeAnnotation.ElementValuePair[theElementValuePairs.size()]), aConstantPool, signatureParser));
        }

        return new BytecodeAnnotationAttributeInfo(theAnnotations.toArray(new BytecodeAnnotation[theAnnotations.size()]));
    }

    private BytecodeCodeAttributeInfo parseCodeAttribute(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {

        int theMaxStack = aDis.readUnsignedShort();
        int theMaxLocals = aDis.readUnsignedShort();
        int theCodeLength = aDis.readInt();
        byte[] theCode = new byte[theCodeLength];
        aDis.readFully(theCode);

        BytecodeProgram theProgramm = programmParser.parse(theCode, aConstantPool);

        int theExceptionTableLength = aDis.readUnsignedShort();
        for (int i=0;i<theExceptionTableLength;i++) {
            BytecodeOpcodeAddress theStartPC = new BytecodeOpcodeAddress(aDis.readUnsignedShort());
            BytecodeOpcodeAddress theEndPc = new BytecodeOpcodeAddress(aDis.readUnsignedShort());
            BytecodeOpcodeAddress theHandlerPc = new BytecodeOpcodeAddress(aDis.readUnsignedShort());
            int theCatchType = aDis.readUnsignedShort();
            theProgramm.addExceptionHandler(new BytecodeExceptionTableEntry(theStartPC, theEndPc, theHandlerPc, theCatchType, aConstantPool));
        }
        BytecodeAttributeInfo[] theAttributes = parseAttributes(aDis, aConstantPool);

        return new BytecodeCodeAttributeInfo(theMaxStack, theMaxLocals, theProgramm, theAttributes);
    }

    private BytecodeAttributeInfo[] parseAttributes(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        List<BytecodeAttributeInfo> theAttributes = new ArrayList<>();
        int theAttributesCount = aDis.readUnsignedShort();
        for (int j=0;j<theAttributesCount;j++) {
            int theAttributeNameIndex = aDis.readUnsignedShort();

            BytecodeConstant theAttributeNameConstant = aConstantPool.constantByIndex(theAttributeNameIndex - 1);
            if (!(theAttributeNameConstant instanceof BytecodeUtf8Constant)) {
                throw new IllegalStateException("Invalid constant reference : got type " + theAttributeNameConstant.getClass().getName());
            }

            int theAttributeLength = aDis.readInt();
            switch (((BytecodeUtf8Constant) theAttributeNameConstant).stringValue()) {
            case "Code":
                theAttributes.add(parseCodeAttribute(aDis, aConstantPool));
                break;
            case "RuntimeVisibleAnnotations":
                theAttributes.add(parseAnnotationAttribute(aDis, aConstantPool));
                break;
            case "BootstrapMethods":
                theAttributes.add(parseBootstrapAttribute(aDis, aConstantPool));
                break;
            case "LocalVariableTable":
                theAttributes.add(parseLocalVariableTableAttribute(aDis, aConstantPool));
                break;
            default:
                byte[] theAttributeData = new byte[theAttributeLength];
                aDis.readFully(theAttributeData);

                theAttributes.add(new BytecodeUnknownAttributeInfo((BytecodeUtf8Constant) theAttributeNameConstant, theAttributeData));
                break;
            }
        }
        return theAttributes.toArray(new BytecodeAttributeInfo[theAttributes.size()]);
    }

    private BytecodeField[] parseFields(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        List<BytecodeField> theFields = new ArrayList<>();
        int theFieldCount = aDis.readUnsignedShort();
        for (int i=0;i<theFieldCount;i++) {
            int theAccessFlags = aDis.readUnsignedShort();
            int theNameIndex = aDis.readUnsignedShort();
            BytecodeConstant theNameConstant = aConstantPool.constantByIndex(theNameIndex - 1);
            if (!(theNameConstant instanceof BytecodeUtf8Constant)) {
                throw new IllegalStateException("Invalid interface constant reference : got type " + theNameConstant.getClass().getName());
            }

            int theDescriptorIndex = aDis.readUnsignedShort();
            BytecodeConstant theDescriptorConstant = aConstantPool.constantByIndex(theDescriptorIndex - 1);
            if (!(theDescriptorConstant instanceof BytecodeUtf8Constant)) {
                throw new IllegalStateException("Invalid interface constant reference : got type " + theDescriptorConstant.getClass().getName());
            }

            BytecodeAttributeInfo[] theAttributes = parseAttributes(aDis, aConstantPool);

            BytecodeTypeRef theTypeRef = signatureParser.toFieldType(((BytecodeUtf8Constant) theDescriptorConstant));

            theFields.add(new BytecodeField(
                    new BytecodeAccessFlags(theAccessFlags),
                    (BytecodeUtf8Constant) theNameConstant,
                    theTypeRef,
                    theAttributes));
        }
        return theFields.toArray(new BytecodeField[theFields.size()]);
    }

    private BytecodeMethod[] parseMethods(DataInput aDis, BytecodeConstantPool aConstantPool) throws IOException {
        List<BytecodeMethod> theMethods = new ArrayList<>();
        int theMethodCount = aDis.readUnsignedShort();
        for (int i=0;i<theMethodCount;i++) {
            int theAccessFlags = aDis.readUnsignedShort();
            int theNameIndex = aDis.readUnsignedShort();

            BytecodeConstant theName = aConstantPool.constantByIndex(theNameIndex - 1);
            if (!(theName instanceof BytecodeUtf8Constant)) {
                throw new IllegalStateException("Invalid interface constant reference : got type " + theName.getClass().getName());
            }

            int theDescriptorIndex = aDis.readUnsignedShort();

            BytecodeConstant theDescriptor = aConstantPool.constantByIndex(theDescriptorIndex - 1);
            if (!(theDescriptor instanceof BytecodeUtf8Constant)) {
                throw new IllegalStateException("Invalid interface constant reference : got type " + theDescriptor.getClass().getName());
            }

            BytecodeAttributeInfo[] theAttributes = parseAttributes(aDis, aConstantPool);

            theMethods.add(new BytecodeMethod(new BytecodeAccessFlags(theAccessFlags),
                    (BytecodeUtf8Constant) theName,
                    signatureParser.toMethodSignature((BytecodeUtf8Constant) theDescriptor),
                    theAttributes));
        }
        return theMethods.toArray(new BytecodeMethod[theMethods.size()]);
    }
}