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
package de.mirkosertic.bytecoder.backend.js;

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.mirkosertic.bytecoder.backend.CompileOptions;
import de.mirkosertic.bytecoder.backend.ConstantPool;
import de.mirkosertic.bytecoder.backend.IndentSSAWriter;
import de.mirkosertic.bytecoder.core.BytecodeFieldRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;
import de.mirkosertic.bytecoder.core.BytecodeVirtualMethodIdentifier;
import de.mirkosertic.bytecoder.relooper.Relooper;
import de.mirkosertic.bytecoder.ssa.ArrayEntryExpression;
import de.mirkosertic.bytecoder.ssa.ArrayLengthExpression;
import de.mirkosertic.bytecoder.ssa.ArrayStoreExpression;
import de.mirkosertic.bytecoder.ssa.BinaryExpression;
import de.mirkosertic.bytecoder.ssa.BreakExpression;
import de.mirkosertic.bytecoder.ssa.ByteValue;
import de.mirkosertic.bytecoder.ssa.CheckCastExpression;
import de.mirkosertic.bytecoder.ssa.ClassReferenceValue;
import de.mirkosertic.bytecoder.ssa.CompareExpression;
import de.mirkosertic.bytecoder.ssa.ComputedMemoryLocationReadExpression;
import de.mirkosertic.bytecoder.ssa.ComputedMemoryLocationWriteExpression;
import de.mirkosertic.bytecoder.ssa.ContinueExpression;
import de.mirkosertic.bytecoder.ssa.CurrentExceptionExpression;
import de.mirkosertic.bytecoder.ssa.DirectInvokeMethodExpression;
import de.mirkosertic.bytecoder.ssa.DoubleValue;
import de.mirkosertic.bytecoder.ssa.Expression;
import de.mirkosertic.bytecoder.ssa.ExpressionList;
import de.mirkosertic.bytecoder.ssa.FixedBinaryExpression;
import de.mirkosertic.bytecoder.ssa.FloatValue;
import de.mirkosertic.bytecoder.ssa.FloorExpression;
import de.mirkosertic.bytecoder.ssa.GetFieldExpression;
import de.mirkosertic.bytecoder.ssa.GetStaticExpression;
import de.mirkosertic.bytecoder.ssa.GotoExpression;
import de.mirkosertic.bytecoder.ssa.IFExpression;
import de.mirkosertic.bytecoder.ssa.InstanceOfExpression;
import de.mirkosertic.bytecoder.ssa.IntegerValue;
import de.mirkosertic.bytecoder.ssa.InvokeStaticMethodExpression;
import de.mirkosertic.bytecoder.ssa.InvokeVirtualMethodExpression;
import de.mirkosertic.bytecoder.ssa.LongValue;
import de.mirkosertic.bytecoder.ssa.LookupSwitchExpression;
import de.mirkosertic.bytecoder.ssa.MemorySizeExpression;
import de.mirkosertic.bytecoder.ssa.MethodHandlesGeneratedLookupExpression;
import de.mirkosertic.bytecoder.ssa.MethodParameterValue;
import de.mirkosertic.bytecoder.ssa.MethodRefExpression;
import de.mirkosertic.bytecoder.ssa.MethodTypeExpression;
import de.mirkosertic.bytecoder.ssa.NegatedExpression;
import de.mirkosertic.bytecoder.ssa.NewArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewMultiArrayExpression;
import de.mirkosertic.bytecoder.ssa.NewObjectExpression;
import de.mirkosertic.bytecoder.ssa.NullValue;
import de.mirkosertic.bytecoder.ssa.Program;
import de.mirkosertic.bytecoder.ssa.PutFieldExpression;
import de.mirkosertic.bytecoder.ssa.PutStaticExpression;
import de.mirkosertic.bytecoder.ssa.RegionNode;
import de.mirkosertic.bytecoder.ssa.ResolveCallsiteObjectExpression;
import de.mirkosertic.bytecoder.ssa.ReturnExpression;
import de.mirkosertic.bytecoder.ssa.ReturnValueExpression;
import de.mirkosertic.bytecoder.ssa.RuntimeGeneratedTypeExpression;
import de.mirkosertic.bytecoder.ssa.SelfReferenceParameterValue;
import de.mirkosertic.bytecoder.ssa.SetMemoryLocationExpression;
import de.mirkosertic.bytecoder.ssa.ShortValue;
import de.mirkosertic.bytecoder.ssa.SqrtExpression;
import de.mirkosertic.bytecoder.ssa.StackTopExpression;
import de.mirkosertic.bytecoder.ssa.StringValue;
import de.mirkosertic.bytecoder.ssa.TableSwitchExpression;
import de.mirkosertic.bytecoder.ssa.ThrowExpression;
import de.mirkosertic.bytecoder.ssa.TypeConversionExpression;
import de.mirkosertic.bytecoder.ssa.TypeOfExpression;
import de.mirkosertic.bytecoder.ssa.TypeRef;
import de.mirkosertic.bytecoder.ssa.UnknownExpression;
import de.mirkosertic.bytecoder.ssa.UnreachableExpression;
import de.mirkosertic.bytecoder.ssa.Value;
import de.mirkosertic.bytecoder.ssa.Variable;
import de.mirkosertic.bytecoder.ssa.VariableAssignmentExpression;

public class JSSSAWriter extends IndentSSAWriter {

    private final ConstantPool constantPool;

    public JSSSAWriter(CompileOptions aOptions, Program aProgram, String aIndent, PrintWriter aWriter, BytecodeLinkerContext aLinkerContext,
            ConstantPool aConstantPool) {
        super(aOptions, aProgram, aIndent, aWriter, aLinkerContext);
        constantPool = aConstantPool;
    }

    private JSSSAWriter withDeeperIndent() {
        return new JSSSAWriter(options, program, indent + "    ", writer, linkerContext, constantPool);
    }

    private void print(Value aValue) {
        if (aValue instanceof Variable) {
            printVariableName((Variable) aValue);
        } else if (aValue instanceof GetStaticExpression) {
            print((GetStaticExpression) aValue);
        } else if (aValue instanceof NullValue) {
            print((NullValue) aValue);
        } else if (aValue instanceof InvokeVirtualMethodExpression) {
            print((InvokeVirtualMethodExpression) aValue);
        } else if (aValue instanceof InvokeStaticMethodExpression) {
            print((InvokeStaticMethodExpression) aValue);
        } else if (aValue instanceof NewObjectExpression) {
            print((NewObjectExpression) aValue);
        } else if (aValue instanceof ByteValue) {
            print((ByteValue) aValue);
        } else if (aValue instanceof BinaryExpression) {
            print((BinaryExpression) aValue);
        } else if (aValue instanceof GetFieldExpression) {
            print((GetFieldExpression) aValue);
        } else if (aValue instanceof TypeConversionExpression) {
            print((TypeConversionExpression) aValue);
        } else if (aValue instanceof ArrayEntryExpression) {
            print((ArrayEntryExpression) aValue);
        } else if (aValue instanceof ArrayLengthExpression) {
            print((ArrayLengthExpression) aValue);
        } else if (aValue instanceof StringValue) {
            print((StringValue) aValue);
        } else if (aValue instanceof IntegerValue) {
            print((IntegerValue) aValue);
        } else if (aValue instanceof NewArrayExpression) {
            print((NewArrayExpression) aValue);
        } else if (aValue instanceof DirectInvokeMethodExpression) {
            print((DirectInvokeMethodExpression) aValue);
        } else if (aValue instanceof FloatValue) {
            print((FloatValue) aValue);
        } else if (aValue instanceof DoubleValue) {
            print((DoubleValue) aValue);
        } else if (aValue instanceof CompareExpression) {
            print((CompareExpression) aValue);
        } else if (aValue instanceof NegatedExpression) {
            print((NegatedExpression) aValue);
        } else if (aValue instanceof FixedBinaryExpression) {
            print((FixedBinaryExpression) aValue);
        } else if (aValue instanceof ShortValue) {
            print((ShortValue) aValue);
        } else if (aValue instanceof InstanceOfExpression) {
            print((InstanceOfExpression) aValue);
        } else if (aValue instanceof LongValue) {
            print((LongValue) aValue);
        } else if (aValue instanceof ClassReferenceValue) {
            print((ClassReferenceValue) aValue);
        } else if (aValue instanceof NewMultiArrayExpression) {
            print((NewMultiArrayExpression) aValue);
        } else if (aValue instanceof SelfReferenceParameterValue) {
            print((SelfReferenceParameterValue) aValue);
        } else if (aValue instanceof MethodParameterValue) {
            print((MethodParameterValue) aValue);
        } else if (aValue instanceof CurrentExceptionExpression) {
            print((CurrentExceptionExpression) aValue);
        } else if (aValue instanceof UnknownExpression) {
            print((UnknownExpression) aValue);
        } else if (aValue instanceof FloorExpression) {
            print((FloorExpression) aValue);
        } else if (aValue instanceof MethodRefExpression) {
            print((MethodRefExpression) aValue);
        } else if (aValue instanceof ComputedMemoryLocationReadExpression) {
            print((ComputedMemoryLocationReadExpression) aValue);
        } else if (aValue instanceof ComputedMemoryLocationWriteExpression) {
            print((ComputedMemoryLocationWriteExpression) aValue);
        } else if (aValue instanceof MethodHandlesGeneratedLookupExpression) {
            print((MethodHandlesGeneratedLookupExpression) aValue);
        } else if (aValue instanceof MethodTypeExpression) {
            print((MethodTypeExpression) aValue);
        } else if (aValue instanceof RuntimeGeneratedTypeExpression) {
            print((RuntimeGeneratedTypeExpression) aValue);
        } else if (aValue instanceof ResolveCallsiteObjectExpression) {
            print((ResolveCallsiteObjectExpression) aValue);
        } else if (aValue instanceof StackTopExpression) {
            print((StackTopExpression) aValue);
        } else if (aValue instanceof MemorySizeExpression) {
            print((MemorySizeExpression) aValue);
        } else if (aValue instanceof TypeOfExpression) {
            print((TypeOfExpression) aValue);
        } else if (aValue instanceof SqrtExpression) {
            print((SqrtExpression) aValue);
        } else {
            throw new IllegalStateException("Not implemented : " + aValue);
        }
    }

    private void print(SqrtExpression aValue) {
        print("Math.sqrt(");
        print((Value) aValue.incomingDataFlows().get(0));
        print(")");
    }

    private void print(TypeOfExpression aValue) {
        print(aValue.incomingDataFlows().get(0));
        print(".TClassgetClass()");
    }

    private void print(StackTopExpression aValue) {
        print("0");
    }

    private void print(MemorySizeExpression aValue) {
        print("0");
    }

    private void print(ResolveCallsiteObjectExpression aValue) {


        print("bytecoder.resolveStaticCallSiteObject(");
        print(JSWriterUtils.toClassName(aValue.getOwningClass().getThisInfo()));
        print(",'");
        print(aValue.getCallsiteId());
        println("', function() {");

        Program theProgram = aValue.getProgram();
        RegionNode theBootstrapCode = aValue.getBootstrapMethod();

        JSSSAWriter theNested = withDeeperIndent();

        for (Variable theVariable : theProgram.globalVariables()) {
            theNested.print("var ");
            theNested.print(theVariable.getName());
            theNested.println(" = null;");
        }

        theNested.writeExpressions(theBootstrapCode.getExpressions());

        print("})");
    }

    private void print(RuntimeGeneratedTypeExpression aValue) {
        print("bytecoder.dynamicType(");
        print(aValue.getMethodRef());
        print(")");
    }

    private void print(MethodTypeExpression aValue) {
        print("'");
        print(aValue.getSignature().toString());
        print("'");
    }

    private void print(MethodHandlesGeneratedLookupExpression aValue) {
        print("null");
    }

    private void print(ComputedMemoryLocationWriteExpression aValue) {

        List<Value> theIncomingData = aValue.incomingDataFlows();

        print(theIncomingData.get(0));
        print(" + ");
        print(theIncomingData.get(1));
    }

    private void print(ComputedMemoryLocationReadExpression aValue) {

        List<Value> theIncomingData = aValue.incomingDataFlows();

        print("bytecoderGlobalMemory[");
        print(theIncomingData.get(0));
        print(" + ");
        print(theIncomingData.get(1));
        print("]");
    }

    private void print(MethodRefExpression aValue) {
        String theMethodName = aValue.getMethodRef().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();
        BytecodeMethodSignature theSignature = aValue.getMethodRef().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();
        print(JSWriterUtils.toClassName(aValue.getMethodRef().getClassIndex().getClassConstant()));
        print(".");
        print(JSWriterUtils.toMethodName(theMethodName, theSignature));
    }

    private void print(FloorExpression aValue) {
        print("Math.floor(");
        print(aValue.incomingDataFlows().get(0));
        print(")");
    }

    private void print(UnknownExpression aValue) {
        print("undefined");
    }

    private void print(CurrentExceptionExpression aValue) {
        //TODO: Fix this
        print("'current exception'");
    }

    private void print(MethodParameterValue aValue) {
        print("p" + (aValue.getParameterIndex() + 1));
    }

    private void print(SelfReferenceParameterValue aValue) {
        print("thisRef");
    }

    private void print(NewMultiArrayExpression aValue) {
        BytecodeTypeRef theType = aValue.getType();
        Object theDefaultValue = theType.defaultValue();
        String theStrDefault = theDefaultValue != null ? theDefaultValue.toString() : "null";
        print("bytecoder.newMultiArray(");
        print("[");
        List<Value> theDimensions = aValue.incomingDataFlows();
        for (int i=0;i<theDimensions.size();i++) {
            if (i>0) {
                print(",");
            }
            print(theDimensions.get(i));
        }
        print("]");
        print(",");
        print(theStrDefault);
        print(")");
    }

    private void print(ClassReferenceValue aValue) {
        print(JSWriterUtils.toClassName(aValue.getType()));
    }

    private void print(InstanceOfExpression aValue) {
        Value theValue = aValue.incomingDataFlows().get(0);
        print("(");
        print(theValue);
        print(" == null ? false : ");
        print(theValue);
        print(".instanceOf(");

        BytecodeUtf8Constant theConstant = aValue.getType().getConstant();
        if (!theConstant.stringValue().startsWith("[")) {
            BytecodeLinkedClass theLinkedClass = linkerContext.isLinkedOrNull(aValue.getType().getConstant());
            print(JSWriterUtils.toClassName(theLinkedClass.getClassName()));
        } else {
            BytecodeLinkedClass theLinkedClass = linkerContext.resolveClass(BytecodeObjectTypeRef.fromRuntimeClass(Array.class));
            print(JSWriterUtils.toClassName(theLinkedClass.getClassName()));
        }

        print(")");
        print(")");
    }

    private void print(LongValue aValue) {
        print(aValue.getLongValue());
    }

    private void print(ShortValue aValue) {
        print(aValue.getShortValue());
    }

    private void print(NegatedExpression aValue) {
        Value theValue = aValue.incomingDataFlows().get(0);
        print("(-");
        print(theValue);
        print(")");
    }

    private void print(CompareExpression aValue) {

        List<Value> theIncomingData = aValue.incomingDataFlows();

        Value theVariable1 = theIncomingData.get(0);
        Value theVariable2 = theIncomingData.get(1);
        print("(");
        print(theVariable1);
        print(" > ");
        print(theVariable2);
        print(" ? 1 ");
        print(" : (");
        print(theVariable1);
        print(" < ");
        print(theVariable2);
        print(" ? -1 : 0))");
    }

    private void print(NewArrayExpression aValue) {
        BytecodeTypeRef theType = aValue.getType();
        Value theLength =aValue.incomingDataFlows().get(0);
        Object theDefaultValue = theType.defaultValue();
        String theStrDefault = theDefaultValue != null ? theDefaultValue.toString() : "null";
        print("bytecoder.newArray(");
        print(theLength);
        print(",");
        print(theStrDefault);
        print(")");
    }

    private void print(IntegerValue aValue) {
        print(aValue.getIntValue());
    }

    private void print(FloatValue aValue) {
        print(aValue.getFloatValue());
    }

    private void print(DoubleValue aValue) {
        print(aValue.getDoubleValue());
    }

    private void print(StringValue aValue) {
        int theIndex = constantPool.register(aValue);
        print("bytecoder.stringpool[");
        print(theIndex);
        print("]");
    }

    private void print(ArrayLengthExpression aValue) {
        print(aValue.incomingDataFlows().get(0));
        print(".data.length");
    }

    private void printArrayIndexReference(Value aValue) {
        print(".data[");
        print(aValue);
        print("]");
    }

    private void print(ArrayEntryExpression aValue) {

        List<Value> theIncomingData = aValue.incomingDataFlows();

        Value theArray = theIncomingData.get(0);
        Value theIndex = theIncomingData.get(1);
        print(theArray);
        printArrayIndexReference(theIndex);
    }

    private void print(TypeConversionExpression aValue) {
        TypeRef theTargetType = aValue.resolveType();
        Value theValue = aValue.incomingDataFlows().get(0);
        switch (theTargetType.resolve()) {
            case FLOAT:
                print(theValue);
                break;
            case DOUBLE:
                print(theValue);
                break;
            default:
                print("Math.floor(");
                print(theValue);
                print(")");
                break;
        }
    }

    private void print(GetFieldExpression aValue) {
        Value theTarget = aValue.incomingDataFlows().get(0);
        BytecodeFieldRefConstant theField = aValue.getField();
        print(theTarget);
        printInstanceFieldReference(theField);
    }

    private void print(BinaryExpression aValue) {

        List<Value> theIncomingData = aValue.incomingDataFlows();

        print("(");
        print(theIncomingData.get(0));
        switch (aValue.getOperator()) {
            case ADD:
                print(" + ");
                break;
            case DIV:
                print(" / ");
                break;
            case MUL:
                print(" * ");
                break;
            case SUB:
                print(" - ");
                break;
            case EQUALS:
                print(" == ");
                break;
            case BINARYOR:
                print(" | ");
                break;
            case LESSTHAN:
                print(" < ");
                break;
            case BINARYAND:
                print(" & ");
                break;
            case BINARYXOR:
                print(" ^ ");
                break;
            case NOTEQUALS:
                print(" != ");
                break;
            case REMAINDER:
                print(" % ");
                break;
            case GREATERTHAN:
                print(" > ");
                break;
            case BINARYSHIFTLEFT:
                print(" << ");
                break;
            case GREATEROREQUALS:
                print(" >= ");
                break;
            case BINARYSHIFTRIGHT:
                print(" >> ");
                break;
            case LESSTHANOREQUALS:
                print(" <= ");
                break;
            case BINARYUNSIGNEDSHIFTRIGHT:
                print(" >>> ");
                break;
            default:
                throw new IllegalStateException("Unsupported operator : " + aValue.getOperator());
        }
        print(theIncomingData.get(1));
        print(")");
    }

    private void print(FixedBinaryExpression aValue) {
        Value theValue1 = aValue.incomingDataFlows().get(0);
        print(theValue1);
        switch (aValue.getOperator()) {
            case ISNONNULL:
                print(" != null ");
                break;
            case ISZERO:
                print(" == 0 ");
                break;
            case ISNULL:
                print(" == null ");
                break;
            default:
                throw new IllegalStateException("Unsupported operator : " + aValue.getOperator());
        }
    }

    private void print(ByteValue aValue) {
        print(aValue.getByteValue());
    }

    private void print(NewObjectExpression aValue) {
        print("new ");
        print(JSWriterUtils.toClassName(aValue.getType()));
        print(".Create()");
    }

    private void print(InvokeStaticMethodExpression aValue) {
        String theMethodName = aValue.getMethodName();
        BytecodeMethodSignature theSignature = aValue.getSignature();

        List<Value> theVariables = aValue.incomingDataFlows();

        print(JSWriterUtils.toClassName(aValue.getClassName()));
        print(".");
        print(JSWriterUtils.toMethodName(theMethodName, theSignature));
        print("(");

        for (int i = 0; i < theVariables.size(); i++) {
            if (i> 0) {
                print(",");
            }
            print(theVariables.get(i));
        }
        print(")");
    }

    private void print(DirectInvokeMethodExpression aValue) {

        String theMethodName = aValue.getMethodName();
        BytecodeMethodSignature theSignature = aValue.getSignature();

        List<Value> theIncomingData = aValue.incomingDataFlows();

        Value theTarget = theIncomingData.get(0);
        List<Value> theArguments = theIncomingData.subList(1, theIncomingData.size());

        if (!"<init>".equals(theMethodName)) {
            print(theTarget);
            print(".");
            print(JSWriterUtils.toMethodName(theMethodName, theSignature));
        } else {
            print(JSWriterUtils.toClassName(aValue.getClazz()));
            print(".");
            print(JSWriterUtils.toMethodName(theMethodName, theSignature));
        }
        print("(");

        print(theTarget);

        for (Value theArgument : theArguments) {
            print(",");
            print(theArgument);
        }
        print(")");
    }

    private void print(InvokeVirtualMethodExpression aValue) {
        String theMethodName = aValue.getMethodName();
        BytecodeMethodSignature theSignature = aValue.getSignature();

        List<Value> theIncomingData = aValue.incomingDataFlows();

        Value theTarget = theIncomingData.get(0);
        List<Value> theArguments = theIncomingData.subList(1, theIncomingData.size());

        BytecodeVirtualMethodIdentifier theMethodIdentifier = linkerContext.getMethodCollection().identifierFor(theMethodName, theSignature);

        if (Objects.equals(aValue.getMethodName(), "invokeWithMagicBehindTheScenes")) {
            print("(");
        } else {
            print(theTarget);
            print(".");
            print(JSWriterUtils.toMethodName(theMethodName, theSignature));
            print("(");
        }

        print(theTarget);
        for (Value theArgument : theArguments) {
            print(",");
            print(theArgument);
        }
        print(")");
    }

    private void print(NullValue aValue) {
        print("null");
    }

    private void print(GetStaticExpression aValue) {
        printStaticFieldReference(aValue.getField());
    }

    private void printVariableName(Variable aVariable) {
        print(aVariable.getName());
    }

    private void printStaticFieldReference(BytecodeFieldRefConstant aField) {
        print(JSWriterUtils.toClassName(aField.getClassIndex().getClassConstant()));
        print(".");
        print(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
    }

    private void printInstanceFieldReference(BytecodeFieldRefConstant aField) {
        print(".");
        print(aField.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue());
    }

    private String generateJumpCodeFor(BytecodeOpcodeAddress aTarget) {
        return "currentLabel = " + aTarget.getAddress()+";continue controlflowloop;";
    }

    private void writeExpressions(ExpressionList aExpressions) {
        for (Expression theExpression : aExpressions.toList()) {
            if (options.isDebugOutput()) {
                String theComment = theExpression.getComment();
                if (theComment != null && !theComment.isEmpty()) {
                    print("// ");
                    println(theComment);
                }
            }
            if (theExpression instanceof ReturnExpression) {
                ReturnExpression theE = (ReturnExpression) theExpression;
                print("return");
                println(";");
            } else if (theExpression instanceof VariableAssignmentExpression) {
                VariableAssignmentExpression theE = (VariableAssignmentExpression) theExpression;

                Variable theVariable = theE.getVariable();
                Value theValue = theE.getValue();

                if (theValue instanceof ComputedMemoryLocationWriteExpression) {
                    continue;
                }
                if (!program.isGlobalVariable(theVariable)) {
                    print("var ");
                }

                print(theVariable.getName());
                print(" = ");
                print(theValue);
                print("; // type is ");
                println(theVariable.resolveType().resolve().name() + " value type is " + theValue.resolveType());
            } else if (theExpression instanceof PutStaticExpression) {
                PutStaticExpression theE = (PutStaticExpression) theExpression;
                BytecodeFieldRefConstant theField = theE.getField();
                Value theValue = theE.incomingDataFlows().get(0);
                printStaticFieldReference(theField);
                print(" = ");
                print(theValue);
                println(";");
            } else if (theExpression instanceof ReturnValueExpression) {
                ReturnValueExpression theE = (ReturnValueExpression) theExpression;
                Value theValue = theE.incomingDataFlows().get(0);
                print("return ");
                print(theValue);
                println(";");
            } else if (theExpression instanceof ThrowExpression) {
                ThrowExpression theE = (ThrowExpression) theExpression;
                Value theValue = theE.incomingDataFlows().get(0);
                print("throw ");
                print(theValue);
                println(";");
            } else if (theExpression instanceof InvokeVirtualMethodExpression) {
                InvokeVirtualMethodExpression theE = (InvokeVirtualMethodExpression) theExpression;
                print(theE);
                println(";");
            } else if (theExpression instanceof DirectInvokeMethodExpression) {
                DirectInvokeMethodExpression theE = (DirectInvokeMethodExpression) theExpression;
                print(theE);
                println(";");
            } else if (theExpression instanceof InvokeStaticMethodExpression) {
                InvokeStaticMethodExpression theE = (InvokeStaticMethodExpression) theExpression;
                print(theE);
                println(";");
            } else if (theExpression instanceof PutFieldExpression) {
                PutFieldExpression theE = (PutFieldExpression) theExpression;

                List<Value> theIncomingData = theE.incomingDataFlows();

                Value theTarget = theIncomingData.get(0);
                BytecodeFieldRefConstant theField = theE.getField();

                Value thevalue = theIncomingData.get(1);
                print(theTarget);
                printInstanceFieldReference(theField);
                print(" = ");
                print(thevalue);
                println(";");
            } else if (theExpression instanceof IFExpression) {
                IFExpression theE = (IFExpression) theExpression;
                print("if (");
                print(theE.incomingDataFlows().get(0));
                println(") {");

                withDeeperIndent().writeExpressions(theE.getExpressions());

                println("}");

            } else if (theExpression instanceof GotoExpression) {
                GotoExpression theE = (GotoExpression) theExpression;
                println(generateJumpCodeFor(theE.getJumpTarget()));
            } else if (theExpression instanceof ArrayStoreExpression) {
                ArrayStoreExpression theE = (ArrayStoreExpression) theExpression;

                List<Value> theIncomingData = theE.incomingDataFlows();

                Value theArray = theIncomingData.get(0);
                Value theIndex = theIncomingData.get(1);
                Value theValue = theIncomingData.get(2);
                print(theArray);
                printArrayIndexReference(theIndex);
                print(" = ");
                print(theValue);
                println(";");
            } else if (theExpression instanceof CheckCastExpression) {
                CheckCastExpression theE = (CheckCastExpression) theExpression;
                // Completely ignored
            } else if (theExpression instanceof TableSwitchExpression) {
                TableSwitchExpression theE = (TableSwitchExpression) theExpression;
                Value theValue = theE.incomingDataFlows().get(0);

                print("if (");
                print(theValue);
                print(" < ");
                print(theE.getLowValue());
                print(" || ");
                print(theValue);
                print(" > ");
                print(theE.getHighValue());
                println(") {");
                print(" ");

                writeExpressions(theE.getDefaultExpressions());

                println("}");
                print("switch(");
                print(theValue);
                print(" - ");
                print(theE.getLowValue());
                println(") {");

                for (Map.Entry<Long, ExpressionList> theEntry : theE.getOffsets().entrySet()) {
                    print(" case ");
                    print(theEntry.getKey());
                    println(":");
                    print("     ");
                    writeExpressions(theEntry.getValue());
                }

                println("}");
                println("throw 'Illegal jump target!';");
            } else if (theExpression instanceof LookupSwitchExpression) {
                LookupSwitchExpression theE = (LookupSwitchExpression) theExpression;
                print("switch(");
                print(theE.incomingDataFlows().get(0));
                println(") {");

                for (Map.Entry<Long, ExpressionList> theEntry : theE.getPairs().entrySet()) {
                    print(" case ");
                    print(theEntry.getKey());
                    println(":");

                    print("     ");
                    writeExpressions(theEntry.getValue());
                }

                println("}");

                writeExpressions(theE.getDefaultExpressions());
            } else if (theExpression instanceof SetMemoryLocationExpression) {
                SetMemoryLocationExpression theE = (SetMemoryLocationExpression) theExpression;

                List<Value> theIncomingData = theE.incomingDataFlows();

                print("bytecoderGlobalMemory[");

                ComputedMemoryLocationWriteExpression theValue = (ComputedMemoryLocationWriteExpression) theIncomingData.get(0);

                print(theValue);

                print("] = ");

                print(theIncomingData.get(1));
                println(";");
            } else if (theExpression instanceof UnreachableExpression) {
                println("throw 'Unreachable';");
            } else if (theExpression instanceof BreakExpression) {
                BreakExpression theBreak = (BreakExpression) theExpression;
                if (theBreak.isSetLabelRequired()) {
                    print("__label__ = ");
                    print(theBreak.jumpTarget().getAddress());
                    println(";");
                }
                if (!theBreak.isSilent()) {
                    print("break $");
                    print(theBreak.blockToBreak().name());
                    println(";");
                }
            } else if (theExpression instanceof ContinueExpression) {
                ContinueExpression theContinue = (ContinueExpression) theExpression;
                print("__label__ = ");
                print(theContinue.jumpTarget().getAddress());
                println(";");
                print("continue $");
                print(theContinue.labelToReturnTo().name());
                println(";");
            } else {
                throw new IllegalStateException("Not implemented : " + theExpression);
            }
        }
    }

    public void printRelooped(Relooper.Block aBlock) {
        println("var __label__ = null;");
        print(aBlock);
    }

    private void print(Relooper.Block aBlock) {
        if (aBlock == null) {
            return;
        }
        if (aBlock instanceof Relooper.SimpleBlock) {
            print((Relooper.SimpleBlock) aBlock);
            return;
        }
        if (aBlock instanceof Relooper.LoopBlock) {
            print((Relooper.LoopBlock) aBlock);
            return;
        }
        if (aBlock instanceof Relooper.MultipleBlock) {
            print((Relooper.MultipleBlock) aBlock);
            return;
        }
        throw new IllegalStateException("Not implemented : " + aBlock);
    }

    private void print(Relooper.SimpleBlock aSimpleBlock) {
        JSSSAWriter theWriter = this;
        if (aSimpleBlock.isLabelRequired()) {
            print("$");
            print(aSimpleBlock.label().name());
            println(" : {");

            theWriter = theWriter.withDeeperIndent();
        }

        theWriter.writeExpressions(aSimpleBlock.internalLabel().getExpressions());

        if (aSimpleBlock.isLabelRequired()) {
            println("}");
        }
        print(aSimpleBlock.next());
    }

    private void print(Relooper.LoopBlock aLoopBlock) {
        if (aLoopBlock.isLabelRequired()) {
            print("$");
            print(aLoopBlock.label().name());
            print(" : ");
        }
        println("for (;;) {");

        JSSSAWriter theDeeper = withDeeperIndent();
        theDeeper.print(aLoopBlock.inner());

        println("}");

        print(aLoopBlock.next());
    }

    private void print(Relooper.MultipleBlock aMultiple) {

        if (aMultiple.isLabelRequired()) {
            print("$");
            print(aMultiple.label().name());
            print(" : ");
        }
        println("for(;;) switch (__label__) {");

        JSSSAWriter theDeeper = withDeeperIndent();
        for (Relooper.Block theHandler : aMultiple.handlers()) {
            for (RegionNode theEntry : theHandler.entries()) {
                theDeeper.print("case ");
                theDeeper.print(theEntry.getStartAddress().getAddress());
                theDeeper.println(" : ");
            }

            JSSSAWriter theHandlerWriter = theDeeper.withDeeperIndent();
            theHandlerWriter.print(theHandler);
        }

        println("}");
        print(aMultiple.next());
    }
}