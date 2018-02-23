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
package de.mirkosertic.bytecoder.ssa;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.mirkosertic.bytecoder.classlib.Address;
import de.mirkosertic.bytecoder.classlib.MemoryManager;
import de.mirkosertic.bytecoder.classlib.java.lang.invoke.TRuntimeGeneratedType;
import de.mirkosertic.bytecoder.core.BytecodeArrayTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeBasicBlock;
import de.mirkosertic.bytecoder.core.BytecodeBootstrapMethod;
import de.mirkosertic.bytecoder.core.BytecodeBootstrapMethodsAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeClass;
import de.mirkosertic.bytecoder.core.BytecodeClassinfoConstant;
import de.mirkosertic.bytecoder.core.BytecodeCodeAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeConstant;
import de.mirkosertic.bytecoder.core.BytecodeDoubleConstant;
import de.mirkosertic.bytecoder.core.BytecodeExceptionTableEntry;
import de.mirkosertic.bytecoder.core.BytecodeFloatConstant;
import de.mirkosertic.bytecoder.core.BytecodeInstruction;
import de.mirkosertic.bytecoder.core.BytecodeInstructionACONSTNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionALOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionANEWARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionARRAYLENGTH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionASTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionATHROW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionBIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionCHECKCAST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionD2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP2;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUP2X1;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUPX1;
import de.mirkosertic.bytecoder.core.BytecodeInstructionDUPX2;
import de.mirkosertic.bytecoder.core.BytecodeInstructionF2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionFCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGETFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGETSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGOTO;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericADD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericAND;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericArrayLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericArraySTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericCMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericDIV;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLDC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericMUL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericNEG;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericOR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericREM;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSHL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSHR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericSUB;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericUSHR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionGenericXOR;
import de.mirkosertic.bytecoder.core.BytecodeInstructionI2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionICONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFACMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFCOND;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFICMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNONNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIFNULL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionIINC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINSTANCEOF;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEDYNAMIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEINTERFACE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESPECIAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKESTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionINVOKEVIRTUAL;
import de.mirkosertic.bytecoder.core.BytecodeInstructionInvoke;
import de.mirkosertic.bytecoder.core.BytecodeInstructionL2Generic;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLCMP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLCONST;
import de.mirkosertic.bytecoder.core.BytecodeInstructionLOOKUPSWITCH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionMONITORENTER;
import de.mirkosertic.bytecoder.core.BytecodeInstructionMONITOREXIT;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEW;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEWARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNEWMULTIARRAY;
import de.mirkosertic.bytecoder.core.BytecodeInstructionNOP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectArrayLOAD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectArraySTORE;
import de.mirkosertic.bytecoder.core.BytecodeInstructionObjectRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPOP;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPOP2;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTFIELD;
import de.mirkosertic.bytecoder.core.BytecodeInstructionPUTSTATIC;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRET;
import de.mirkosertic.bytecoder.core.BytecodeInstructionRETURN;
import de.mirkosertic.bytecoder.core.BytecodeInstructionSIPUSH;
import de.mirkosertic.bytecoder.core.BytecodeInstructionTABLESWITCH;
import de.mirkosertic.bytecoder.core.BytecodeIntegerConstant;
import de.mirkosertic.bytecoder.core.BytecodeInvokeDynamicConstant;
import de.mirkosertic.bytecoder.core.BytecodeLinkedClass;
import de.mirkosertic.bytecoder.core.BytecodeLinkerContext;
import de.mirkosertic.bytecoder.core.BytecodeLocalVariableTableAttributeInfo;
import de.mirkosertic.bytecoder.core.BytecodeLocalVariableTableEntry;
import de.mirkosertic.bytecoder.core.BytecodeLongConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethod;
import de.mirkosertic.bytecoder.core.BytecodeMethodHandleConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodRefConstant;
import de.mirkosertic.bytecoder.core.BytecodeMethodSignature;
import de.mirkosertic.bytecoder.core.BytecodeMethodTypeConstant;
import de.mirkosertic.bytecoder.core.BytecodeObjectTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;
import de.mirkosertic.bytecoder.core.BytecodePrimitiveTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeProgram;
import de.mirkosertic.bytecoder.core.BytecodeReferenceIndex;
import de.mirkosertic.bytecoder.core.BytecodeStringConstant;
import de.mirkosertic.bytecoder.core.BytecodeTypeRef;
import de.mirkosertic.bytecoder.core.BytecodeUtf8Constant;

public final class NaiveProgramGenerator implements ProgramGenerator {

    public final static ProgramGeneratorFactory FACTORY = NaiveProgramGenerator::new;

    private static final class ParsingHelper {

        @FunctionalInterface interface ValueProvider {
            Value resolveValueFor(VariableDescription aDescription);
        }

        private final RegionNode block;
        private final Stack<Value> stack;
        private final Map<Integer, Variable> localVariables;
        private final ValueProvider valueProvider;
        private final BytecodeLocalVariableTableAttributeInfo localVariableTableAttributeInfo;

        private ParsingHelper(BytecodeLocalVariableTableAttributeInfo aDebugInfo, RegionNode aBlock, ValueProvider aValueProvider) {
            stack = new Stack<>();
            block = aBlock;
            localVariables = new HashMap<>();
            valueProvider = aValueProvider;
            localVariableTableAttributeInfo = aDebugInfo;
        }

        public int numberOfLocalVariables() {
            return localVariables.size();
        }

        public Value pop() {
            if (stack.isEmpty()) {
                throw new IllegalStateException("Stack is empty!!!");
            }
            return stack.pop();
        }

        public Value peek() {
            return stack.peek();
        }

        public void push(Value aValue) {
            if (aValue == null) {
                throw new IllegalStateException("Trying to push null in " + this);
            }
            stack.push(aValue);
        }

        public Value getLocalVariable(int aIndex) {
            Variable theValue = localVariables.get(aIndex);
            if (theValue == null) {
                VariableDescription theDesc = new LocalVariableDescription(aIndex);
                theValue = (Variable) valueProvider.resolveValueFor(theDesc);
                if (theValue == null) {
                    throw new IllegalStateException("Value must not be null from provider for " + theDesc);
                }
                block.addToImportedList(theValue, theDesc);
                block.addToExportedList(theValue, theDesc);
                localVariables.put(aIndex, theValue);
            }
            return theValue;
        }

        public Value requestValue(VariableDescription aDescription) {
            if (aDescription instanceof LocalVariableDescription) {
                LocalVariableDescription theDesc = (LocalVariableDescription) aDescription;
                return getLocalVariable(theDesc.getIndex());
            }
            StackVariableDescription theStack = (StackVariableDescription) aDescription;
            if (theStack.getPos() < stack.size()) {
                return stack.get(stack.size() - theStack.getPos() - 1);
            }
            throw new IllegalStateException("Invalid stack index : " + theStack.getPos() + " with total size of " + stack.size());
        }

        public void setLocalVariable(BytecodeOpcodeAddress aInstruction, int aIndex, Value aValue) {
            if (aValue == null) {
                throw new IllegalStateException("local variable " + aIndex + " must not be null in " + this);
            }
            if (localVariableTableAttributeInfo != null) {
                BytecodeLocalVariableTableEntry theEntry = localVariableTableAttributeInfo.matchingEntryFor(aInstruction, aIndex);
                if (theEntry != null) {
                    /*String theVariableName = localVariableTableAttributeInfo.resolveVariableName(theEntry);
                    Variable theGlobal = program.getOrCreateTrulyGlobal(theVariableName, aValue.resolveType());
                    theGlobal.initializeWith(aValue);
                    block.addExpression(new VariableAssignmentExpression(theGlobal, aValue));
                    localVariables.put(aIndex, theGlobal);
                    block.addToExportedList(theGlobal, new LocalVariableDescription(aIndex));
                    return;*/
                }
            }
            // Try to find global variables
            if (!(aValue instanceof Variable)) {
                // Promote value to variable
                aValue = block.newVariable(aValue.resolveType(), aValue);
            }
            localVariables.put(aIndex, (Variable) aValue);
            block.addToExportedList(aValue, new LocalVariableDescription(aIndex));
        }

        public void setStackValue(int aStackPos, Value aValue) {
            List<Value> theValues = new ArrayList<>(stack);
            while (theValues.size() <= aStackPos) {
                theValues.add(null);
            }
            theValues.set(aStackPos, aValue);
            stack.clear();
            stack.addAll(theValues);
        }

        public void finalizeExportState() {
            for (Map.Entry<Integer, Variable> theEntry : localVariables.entrySet()) {
                block.addToExportedList(theEntry.getValue(), new LocalVariableDescription(theEntry.getKey()));
            }
            for (int i=stack.size() - 1 ; i>= 0; i--) {
                // Numbering must be consistent here!!
                block.addToExportedList(stack.get(i), new StackVariableDescription(stack.size() - 1 - i));
            }
        }
    }

    private final BytecodeLinkerContext linkerContext;

    private NaiveProgramGenerator(BytecodeLinkerContext aLinkerContext) {
        linkerContext = aLinkerContext;
    }

    public static class ParsingHelperCache {

        private final BytecodeMethod method;
        private final RegionNode startNode;
        private final Map<RegionNode, ParsingHelper> finalStatesForNodes;
        private final Program program;
        private final BytecodeLocalVariableTableAttributeInfo localVariableTableAttributeInfo;

        public ParsingHelperCache(Program aProgram, BytecodeMethod aMethod, RegionNode aStartNode, BytecodeLocalVariableTableAttributeInfo aLocalVariablesInfo) {
            startNode = aStartNode;
            method = aMethod;
            localVariableTableAttributeInfo = aLocalVariablesInfo;
            finalStatesForNodes = new HashMap<>();
            program = aProgram;
        }

        public void registerFinalStateForNode(RegionNode aNode, ParsingHelper aState) {
            finalStatesForNodes.put(aNode, aState);
        }

        public ParsingHelper resolveFinalStateForNode(RegionNode aGraphNode) {
            if (aGraphNode == null) {
                // No node, so we create the initial state of the whole program
                Map<VariableDescription, Value> theValues = new HashMap<>();

                // At this point, local variables are initialized based on the method signature
                // The stack is empty
                int theCurrentIndex = 0;
                if (!method.getAccessFlags().isStatic()) {
                    LocalVariableDescription theDesc = new LocalVariableDescription(theCurrentIndex);
                    theValues.put(theDesc, program.matchingArgumentOf(theDesc).getVariable());
                    theCurrentIndex++;
                }

                BytecodeTypeRef[] theTypes = method.getSignature().getArguments();
                for (BytecodeTypeRef theRef : theTypes) {
                    LocalVariableDescription theDesc = new LocalVariableDescription(theCurrentIndex);
                    theValues.put(theDesc, program.matchingArgumentOf(theDesc).getVariable());
                    theCurrentIndex++;
                    if (theRef == BytecodePrimitiveTypeRef.LONG || theRef == BytecodePrimitiveTypeRef.DOUBLE) {
                        theCurrentIndex++;
                    }
                }

                ParsingHelper.ValueProvider theProvider = aDescription -> {
                    Value theValue = theValues.get(aDescription);
                    if (theValue == null) {
                        throw new IllegalStateException("No value on cfg enter : " + aDescription);
                    }
                    return theValue;
                };

                return new ParsingHelper(localVariableTableAttributeInfo, startNode, theProvider);
            }
            return finalStatesForNodes.get(aGraphNode);
        }

        private TypeRef widestTypeOf(Collection<Value> aValue) {
            if (aValue.size() == 1) {
                return aValue.iterator().next().resolveType();
            }
            TypeRef.Native theCurrent = null;
            for (Value theValue : aValue) {
                TypeRef.Native theValueType = theValue.resolveType().resolve();
                if (theCurrent == null) {
                    theCurrent = theValueType;
                } else {
                    theCurrent = theCurrent.eventuallyPromoteTo(theValueType);
                }
            }
            return theCurrent;
        }

        public ParsingHelper resolveInitialPHIStateForNode(RegionNode aBlock) {
            ParsingHelper.ValueProvider theProvider = aDescription -> newPHIFor(aBlock.getPredecessorsIgnoringBackEdges(), aDescription, aBlock);

            // We collect the stacks from all predecessor nodes
            Map<StackVariableDescription, Set<Value>> theStackToImport = new HashMap<>();
            int theRequestedStack = -1;
            for (RegionNode thePredecessor : aBlock.getPredecessorsIgnoringBackEdges()) {
                ParsingHelper theHelper = finalStatesForNodes.get(thePredecessor);
                if (!theHelper.stack.isEmpty()) {
                    if (theRequestedStack == -1) {
                        theRequestedStack = theHelper.stack.size();
                    } else {
                        if (theRequestedStack != theHelper.stack.size()) {
                            throw new IllegalStateException("Wrong number of exported stack in " + thePredecessor.getStartAddress().getAddress() + " expected " + theRequestedStack + " got " + theHelper.stack.size());
                        }
                    }
                    for (int i=0;i<theHelper.stack.size();i++) {
                        StackVariableDescription theStackPos = new StackVariableDescription(theHelper.stack.size() - i - 1);
                        Value theStackValue = theHelper.stack.get(i);

                        Set<Value> theKnownValues = theStackToImport.computeIfAbsent(theStackPos, k -> new HashSet<>());
                        theKnownValues.add(theStackValue);
                    }
                }
            }

            ParsingHelper theHelper = new ParsingHelper(localVariableTableAttributeInfo, aBlock, theProvider);

            // Now we import the stack and check if we need to insert phi values
            for (Map.Entry<StackVariableDescription, Set<Value>> theEntry : theStackToImport.entrySet()) {
                Set<Value> theValues = theEntry.getValue();
                if (theValues.size() == 1) {
                    // Only one value, we do not need to insert a phi value
                    Value theSingleValue = theValues.iterator().next();
                    theHelper.setStackValue(theRequestedStack - theEntry.getKey().getPos() - 1, theSingleValue);
                    aBlock.addToImportedList(theSingleValue, theEntry.getKey());
                } else {
                    // We have a PHI value here
                    TypeRef theType = widestTypeOf(theValues);
                    Variable thePHI = aBlock.newImportedVariable(theType, theEntry.getKey());
                    for (Value theValue : theValues) {
                        thePHI.initializeWith(theValue);
                    }
                    theHelper.setStackValue(theRequestedStack - theEntry.getKey().getPos() - 1, thePHI);
                }
            }

            return theHelper;
        }

        private Value newPHIFor(Set<RegionNode> aNodes, VariableDescription aDescription, RegionNode aImportingBlock) {
            Set<Value> theValues = new HashSet<>();
            for (RegionNode thePredecessor : aNodes) {
                ParsingHelper theHelper = finalStatesForNodes.get(thePredecessor);
                if (theHelper == null) {
                    throw new IllegalStateException("No helper for " + thePredecessor.getStartAddress().getAddress());
                }
                theValues.add(theHelper.requestValue(aDescription));
            }
            if (theValues.isEmpty()) {
                throw new IllegalStateException("No values for " + aDescription + " in block " + aImportingBlock.getStartAddress().getAddress());
            }
            if (theValues.size() == 1) {
                Value theValue = theValues.iterator().next();
                aImportingBlock.addToImportedList(theValue, aDescription);
                return theValue;
            }
            TypeRef theType = widestTypeOf(theValues);
            Variable thePHI = aImportingBlock.newImportedVariable(theType, aDescription);
            for (Value theValue : theValues) {
                thePHI.initializeWith(theValue);
            }
            return thePHI;
        }

        public ParsingHelper resolveInitialStateFromPredecessorFor(RegionNode aNode, ParsingHelper aPredecessor) {
            // The node will import the full stack from its predecessor
            ParsingHelper.ValueProvider theProvider = aPredecessor::requestValue;
            ParsingHelper theNew = new ParsingHelper(localVariableTableAttributeInfo, aNode, theProvider);
            Stack<Value> theStackToImport = aPredecessor.stack;
            for (int i=0;i<theStackToImport.size();i++) {
                StackVariableDescription theStackDesc = new StackVariableDescription(theStackToImport.size() - i - 1);
                Value theImportedValue = theStackToImport.get(i);
                theNew.stack.push(theImportedValue);
                aNode.addToImportedList(theImportedValue, theStackDesc);
            }
            return theNew;
        }
    }

    @Override
    public Program generateFrom(BytecodeClass aOwningClass, BytecodeMethod aMethod) {

        BytecodeCodeAttributeInfo theCode = aMethod.getCode(aOwningClass);

        Program theProgram = new Program();

        // Initialize programm arguments
        BytecodeLocalVariableTableAttributeInfo theDebugInfos = null;
        if (theCode != null) {
            theDebugInfos = theCode.attributeByType(BytecodeLocalVariableTableAttributeInfo.class);
        }
        int theCurrentIndex = 0;
        if (!aMethod.getAccessFlags().isStatic()) {
            theProgram.addArgument(new LocalVariableDescription(theCurrentIndex), Variable.createThisRef());
            theCurrentIndex++;
        }
        BytecodeTypeRef[] theTypes = aMethod.getSignature().getArguments();
        for (int i=0;i<theTypes.length;i++) {
            BytecodeTypeRef theRef = theTypes[i];
            if (theDebugInfos != null) {
                BytecodeLocalVariableTableEntry theEntry = theDebugInfos.matchingEntryFor(BytecodeOpcodeAddress.START_AT_ZERO, theCurrentIndex);
                if (theEntry != null) {
                    String theVariableName = theDebugInfos.resolveVariableName(theEntry);
                    theProgram.addArgument(new LocalVariableDescription(theCurrentIndex), Variable.createMethodParameter(i + 1, theVariableName, TypeRef.toType(theTypes[i])));
                } else {
                    theProgram.addArgument(new LocalVariableDescription(theCurrentIndex), Variable.createMethodParameter(i + 1, TypeRef.toType(theTypes[i])));
                }
            } else {
                theProgram.addArgument(new LocalVariableDescription(theCurrentIndex), Variable.createMethodParameter(i + 1, TypeRef.toType(theTypes[i])));
            }

            theCurrentIndex++;
            if (theRef == BytecodePrimitiveTypeRef.LONG || theRef == BytecodePrimitiveTypeRef.DOUBLE) {
                theCurrentIndex++;
            }
        }

        List<BytecodeBasicBlock> theBlocks = new ArrayList<>();
        Function<BytecodeOpcodeAddress, BytecodeBasicBlock> theBasicBlockByAddress = aValue -> {
            for (BytecodeBasicBlock theBlock : theBlocks) {
                if (Objects.equals(aValue, theBlock.getStartAddress())) {
                    return theBlock;
                }
            }
            throw new IllegalStateException("No Block for " + aValue.getAddress());
        };

        if (aMethod.getAccessFlags().isAbstract() || aMethod.getAccessFlags().isNative()) {
            return theProgram;
        }

        BytecodeProgram theBytecode = theCode.getProgramm();
        Set<BytecodeOpcodeAddress> theJumpTarget = theBytecode.getJumpTargets();
        BytecodeBasicBlock currentBlock = null;
        for (BytecodeInstruction theInstruction : theBytecode.getInstructions()) {
            if (theJumpTarget.contains(theInstruction.getOpcodeAddress())) {
                // Jump target, start a new basic block
                currentBlock = null;
            }
            if (theBytecode.isStartOfTryBlock(theInstruction.getOpcodeAddress())) {
                // start of try block, hence new basic block
                currentBlock = null;
            }
            if (currentBlock == null) {
                BytecodeBasicBlock.Type theType = BytecodeBasicBlock.Type.NORMAL;
                for (BytecodeExceptionTableEntry theHandler : theBytecode.getExceptionHandlers()) {
                    if (Objects.equals(theHandler.getHandlerPc(), theInstruction.getOpcodeAddress())) {
                        if (theHandler.isFinally()) {
                            theType = BytecodeBasicBlock.Type.FINALLY;
                        } else {
                            theType = BytecodeBasicBlock.Type.EXCEPTION_HANDLER;
                        }
                    }
                }
                BytecodeBasicBlock theCurrentTemp = currentBlock;
                currentBlock = new BytecodeBasicBlock(theType);
                if (theCurrentTemp != null && !theCurrentTemp.endsWithReturn() && !theCurrentTemp.endsWithThrow() && theCurrentTemp.endsWithGoto() && !theCurrentTemp.endsWithConditionalJump()) {
                    theCurrentTemp.addSuccessor(currentBlock);
                }
                theBlocks.add(currentBlock);
            }
            currentBlock.addInstruction(theInstruction);
            if (theInstruction.isJumpSource()) {
                // conditional or unconditional jump, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionRET) {
                // returning, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionRETURN) {
                // returning, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionObjectRETURN) {
                // returning, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                // returning, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                // thowing an exception, start new basic block
                currentBlock = null;
            } else if (theInstruction instanceof BytecodeInstructionInvoke) {
                // invocation, start new basic block
                // currentBlock = null;
            }
        }

        // Now, we have to build the successors of each block
        for (int i=0;i<theBlocks.size();i++) {
            BytecodeBasicBlock theBlock = theBlocks.get(i);
            if (!theBlock.endsWithReturn() && !theBlock.endsWithThrow()) {
                if (theBlock.endsWithJump()) {
                    for (BytecodeInstruction theInstruction : theBlock.getInstructions()) {
                        if (theInstruction.isJumpSource()) {
                            for (BytecodeOpcodeAddress theBlockJumpTarget : theInstruction.getPotentialJumpTargets()) {
                                theBlock.addSuccessor(theBasicBlockByAddress.apply(theBlockJumpTarget));
                            }
                        }
                    }
                    if (theBlock.endsWithConditionalJump()) {
                        if (i<theBlocks.size()-1) {
                            theBlock.addSuccessor(theBlocks.get(i + 1));
                        } else {
                            throw new IllegalStateException("Block at end with no jump target!");
                        }
                    }
                } else {
                    if (i<theBlocks.size()-1) {
                        theBlock.addSuccessor(theBlocks.get(i + 1));
                    } else {
                        throw new IllegalStateException("Block at end with no jump target!");
                    }
                }
            }
        }

        // Ok, now we transform it to GraphNodes with yet empty content
        Map<BytecodeBasicBlock, RegionNode> theCreatedBlocks = new HashMap<>();

        ControlFlowGraph theGraph = theProgram.getControlFlowGraph();
        for (BytecodeBasicBlock theBlock : theBlocks) {
            RegionNode theSingleAssignmentBlock;
            switch (theBlock.getType()) {
            case NORMAL:
                theSingleAssignmentBlock = theGraph.createAt(theBlock.getStartAddress(), RegionNode.BlockType.NORMAL);
                break;
            case EXCEPTION_HANDLER:
                theSingleAssignmentBlock = theGraph.createAt(theBlock.getStartAddress(), RegionNode.BlockType.EXCEPTION_HANDLER);
                break;
            case FINALLY:
                theSingleAssignmentBlock = theGraph.createAt(theBlock.getStartAddress(), RegionNode.BlockType.FINALLY);
                break;
            default:
                throw new IllegalStateException("Unsupported block type : " + theBlock.getType());
            }
            theCreatedBlocks.put(theBlock, theSingleAssignmentBlock);
        }

        // Initialize Block dependency graph
        for (Map.Entry<BytecodeBasicBlock, RegionNode> theEntry : theCreatedBlocks.entrySet()) {
            for (BytecodeBasicBlock theSuccessor : theEntry.getKey().getSuccessors()) {
                RegionNode theSuccessorBlock = theCreatedBlocks.get(theSuccessor);
                if (theSuccessorBlock == null) {
                    throw new IllegalStateException("Cannot find successor block");
                }
                theEntry.getValue().addSuccessor(theSuccessorBlock);
            }
        }

        // And add dependencies for exception handlers and finally blocks
        for (BytecodeExceptionTableEntry theHandler : theBytecode.getExceptionHandlers()) {
            RegionNode theStart = theProgram.getControlFlowGraph().nodeStartingAt(theHandler.getStartPC());
            RegionNode theHandlerNode = theProgram.getControlFlowGraph().nodeStartingAt(theHandler.getHandlerPc());
            theStart.addSuccessor(theHandlerNode);
        }

        // Now we can add the SSA instructions to the graph nodes
        Set<RegionNode> theVisited = new HashSet<>();
        RegionNode theStart = theProgram.getControlFlowGraph().startNode();

        // First of all, we need to mark the back-edges of the graph
        theProgram.getControlFlowGraph().calculateReachabilityAndMarkBackEdges();

        try {
            // Now we can continue to create the program flow
            ParsingHelperCache theParsingHelperCache = new ParsingHelperCache(theProgram, aMethod, theStart, theDebugInfos);

            // This will traverse the CFG from bottom to top
            for (RegionNode theNode : theProgram.getControlFlowGraph().finalNodes()) {
                initializeBlock(theProgram, aOwningClass, aMethod, theNode, theVisited, theParsingHelperCache,
                            theBasicBlockByAddress);
            }

            // Finally, we have to check for blocks what were not directly accessible, for instance exception handlers or
            // finally blocks
            for (Map.Entry<BytecodeBasicBlock, RegionNode> theEntry : theCreatedBlocks.entrySet()) {
                RegionNode theBlock = theEntry.getValue();
                if (theBlock.getType() != RegionNode.BlockType.NORMAL) {
                    initializeBlock(theProgram, aOwningClass, aMethod, theBlock, theVisited, theParsingHelperCache, theBasicBlockByAddress);
                }
            }

            // Check if there are infinite looping blocks
            // Additionally, we have to add gotos
            for (RegionNode theNode : theProgram.getControlFlowGraph().getKnownNodes()) {
                ExpressionList theCurrentList = theNode.getExpressions();
                Expression theLast = theCurrentList.lastExpression();
                if (theLast instanceof GotoExpression) {
                    GotoExpression theGoto = (GotoExpression) theLast;
                    if (Objects.equals(theGoto.getJumpTarget(), theNode.getStartAddress())) {
                        theCurrentList.remove(theGoto);
                    }
                }
                if (!theNode.getExpressions().endWithNeverReturningExpression()) {
                    Map<RegionNode.Edge, RegionNode> theSuccessors = theNode.getSuccessors();
                    for (Expression theExpression : theCurrentList.toList()) {
                        if (theExpression instanceof IFExpression) {
                            IFExpression theIF = (IFExpression) theExpression;
                            BytecodeOpcodeAddress theGoto = theIF.getGotoAddress();
                            theSuccessors = theSuccessors.entrySet().stream().filter(t -> !Objects
                                    .equals(t.getValue().getStartAddress(), theGoto)).collect(
                                    Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                        }
                    }

                    List<RegionNode> theSuccessorRegions = theSuccessors.values().stream().filter(t -> t.getType() == RegionNode.BlockType.NORMAL).collect(
                            Collectors.toList());

                    if (theSuccessorRegions.size() == 1) {
                        theNode.getExpressions().add(new GotoExpression(theSuccessorRegions.get(0).getStartAddress()).withComment("Resolving pass thru direct"));
                    } else {
                        throw new IllegalStateException("Invalid number of successors : " + theSuccessors.size() + " for " + theNode.getStartAddress().getAddress());
                    }
                }
            }

            // Check that all PHI-propagations for back-edges are set
            for (RegionNode theNode : theProgram.getControlFlowGraph().getKnownNodes()) {
                ParsingHelper theHelper = theParsingHelperCache.resolveFinalStateForNode(theNode);
                for (Map.Entry<RegionNode.Edge, RegionNode> theEdge : theNode.getSuccessors().entrySet()) {
                    if (theEdge.getKey().getType() == RegionNode.EdgeType.BACK) {
                        RegionNode theReceiving = theEdge.getValue();
                        BlockState theReceivingState = theReceiving.toStartState();
                        for (Map.Entry<VariableDescription, Value> theEntry : theReceivingState.getPorts().entrySet()) {
                            Value theExportingValue = theHelper.requestValue(theEntry.getKey());
                            if (theExportingValue == null) {
                                throw new IllegalStateException("No value for " + theEntry.getKey() + " to jump from " + theNode.getStartAddress().getAddress() + " to " + theReceiving.getStartAddress().getAddress());
                            }
                            Variable theReceivingTarget = (Variable) theEntry.getValue();
                            theReceivingTarget.initializeWith(theExportingValue);
                        }
                    }
                }
            }

            // Make sure that all jump conditions are met
            for (RegionNode theNode : theProgram.getControlFlowGraph().getKnownNodes()) {
                forEachExpressionOf(theNode, aPoint -> {
                    if (aPoint.expression instanceof GotoExpression) {
                        GotoExpression theGoto = (GotoExpression) aPoint.expression;
                        RegionNode theGotoNode = theProgram.getControlFlowGraph().nodeStartingAt(theGoto.getJumpTarget());
                        BlockState theImportingState = theGotoNode.toStartState();
                        for (Map.Entry<VariableDescription, Value> theImporting : theImportingState.getPorts().entrySet()) {
                            ParsingHelper theHelper = theParsingHelperCache.resolveFinalStateForNode(theNode);
                            Value theExportingValue = theHelper.requestValue(theImporting.getKey());
                            if (theExportingValue == null) {
                                throw new IllegalStateException("No value for " + theImporting.getKey() + " to jump from " + theNode.getStartAddress().getAddress() + " to " + theGotoNode.getStartAddress().getAddress());
                            }
                        }
                    }
                });
            }

            // Insert PHI value resolving at required places
            for (RegionNode theNode : theProgram.getControlFlowGraph().getKnownNodes()) {
                forEachExpressionOf(theNode, aPoint -> {
                    if (aPoint.expression instanceof GotoExpression) {
                        GotoExpression theGoto = (GotoExpression) aPoint.expression;
                        RegionNode theGotoNode = theProgram.getControlFlowGraph().nodeStartingAt(theGoto.getJumpTarget());
                        BlockState theImportingState = theGotoNode.toStartState();
                        String theComments = "";
                        for (Map.Entry<VariableDescription, Value> theImporting : theImportingState.getPorts().entrySet()) {
                            theComments = theComments + theImporting.getKey() + " is of type " + theImporting.getValue().resolveType().resolve()+ " with values " + theImporting.getValue().incomingDataFlows();
                            Value theReceivingValue = theImporting.getValue();
                            ParsingHelper theHelper = theParsingHelperCache.resolveFinalStateForNode(theNode);
                            Value theExportingValue = theHelper.requestValue(theImporting.getKey());
                            if (theExportingValue == null) {
                                throw new IllegalStateException("No value for " + theImporting.getKey() + " to jump from " + theNode.getStartAddress().getAddress() + " to " + theGotoNode.getStartAddress().getAddress());
                            }
                            if (theReceivingValue != theExportingValue) {
                                VariableAssignmentExpression theInit = new VariableAssignmentExpression((Variable) theReceivingValue, theExportingValue);
                                aPoint.expressionList.addBefore(theInit, theGoto);
                            }
                        }
                        theGoto.withComment(theComments);
                    }
                });
            }

        } catch (Exception e) {
            throw new ControlFlowProcessingException("Error processing CFG for " + aOwningClass.getThisInfo().getConstant().stringValue() + "." + aMethod.getName().stringValue(), e, theProgram.getControlFlowGraph());
        }

        return theProgram;
    }

    static class TraversalPoint {
        public final ExpressionList expressionList;
        public final Expression expression;

        public TraversalPoint(ExpressionList aExpressionList, Expression aExpression) {
            expressionList = aExpressionList;
            expression = aExpression;
        }
    }

    public void forEachExpressionOf(RegionNode aNode, Consumer<TraversalPoint> aConsumer)  {
        forEachExpressionOf(aNode.getExpressions(), aConsumer);
    }

    public void forEachExpressionOf(ExpressionList aList, Consumer<TraversalPoint> aConsumer) {
        for (Expression theExpression : aList.toList()) {
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theList : theContainer.getExpressionLists()) {
                    forEachExpressionOf(theList, aConsumer);
                }
            }

            aConsumer.accept(new TraversalPoint(aList, theExpression));
        }
    }

    private void initializeBlock(Program aProgram, BytecodeClass aOwningClass, BytecodeMethod aMethod, RegionNode aCurrentBlock, Set<RegionNode> aAlreadyVisited, ParsingHelperCache aCache, Function<BytecodeOpcodeAddress, BytecodeBasicBlock> aBlocksByAddress) {

        if (aAlreadyVisited.add(aCurrentBlock)) {

            // Resolve predecessor nodes. without them we would not have an initial state for the current node
            // We have to ignore back edges!!
            Set<RegionNode> thePredecessors = aCurrentBlock.getPredecessorsIgnoringBackEdges();
            for (RegionNode thePredecessor : thePredecessors) {
                initializeBlock(aProgram, aOwningClass, aMethod, thePredecessor, aAlreadyVisited, aCache, aBlocksByAddress);
            }

            ParsingHelper theParsingState;
            if (aCurrentBlock.getType() != RegionNode.BlockType.NORMAL) {
                // Exception handler or finally code
                // We only have the thrown exception on the stack!
                // Everything else is at the same state as on control flow enter
                // In case of synchronized blocks there is an additional reference with the semaphore to release
                if (thePredecessors.size() == 1) {
                    RegionNode thePredecessor = thePredecessors.iterator().next();
                    theParsingState = aCache.resolveFinalStateForNode(thePredecessor);
                } else {
                    theParsingState = aCache.resolveInitialPHIStateForNode(aCurrentBlock);
                }
                theParsingState.setLocalVariable(aCurrentBlock.getStartAddress(), theParsingState.numberOfLocalVariables(), Variable.createThisRef());
                theParsingState.push(aCurrentBlock.newVariable(TypeRef.toType(BytecodeObjectTypeRef.fromRuntimeClass(Exception.class)), new CurrentExceptionExpression()));
            } else if (aCurrentBlock.getStartAddress().getAddress() == 0) {
                // Programm is at start address, so we need the initial state
                theParsingState = aCache.resolveFinalStateForNode(null);
            } else if (thePredecessors.size() == 1) {
                // Only one predecessor
                RegionNode thePredecessor = thePredecessors.iterator().next();
                ParsingHelper theResolved = aCache.resolveFinalStateForNode(thePredecessor);
                if (theResolved == null) {
                    throw new IllegalStateException("No fully resolved predecessor found!");
                }
                theParsingState = aCache.resolveInitialStateFromPredecessorFor(aCurrentBlock, theResolved);
            } else {
                // we have more than one predecessor
                // we need to create PHI functions for all the disjunct states in local variables and the stack
                theParsingState = aCache.resolveInitialPHIStateForNode(aCurrentBlock);
            }

            initializeBlockWith(aOwningClass, aMethod, aCurrentBlock, aBlocksByAddress, theParsingState);

            // register the final state after program flow
            aCache.registerFinalStateForNode(aCurrentBlock, theParsingState);
        }
    }

    private void initializeBlockWith(BytecodeClass aOwningClass, BytecodeMethod aMethod, RegionNode aTargetBlock, Function<BytecodeOpcodeAddress, BytecodeBasicBlock> aBlocksByAddress,  ParsingHelper aHelper) {

        // Finally we can start to parse the program
        BytecodeBasicBlock theBytecodeBlock = aBlocksByAddress.apply(aTargetBlock.getStartAddress());

        for (BytecodeInstruction theInstruction : theBytecodeBlock.getInstructions()) {

            if (theInstruction instanceof BytecodeInstructionNOP) {
                BytecodeInstructionNOP theINS = (BytecodeInstructionNOP) theInstruction;
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionMONITORENTER) {
                BytecodeInstructionMONITORENTER theINS = (BytecodeInstructionMONITORENTER) theInstruction;
                // Pop the reference for the lock from the stack
                aHelper.pop();
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionMONITOREXIT) {
                BytecodeInstructionMONITOREXIT theINS = (BytecodeInstructionMONITOREXIT) theInstruction;
                // Pop the reference for the lock from the stack
                aHelper.pop();
                // Completely ignored
            } else if (theInstruction instanceof BytecodeInstructionCHECKCAST) {
                BytecodeInstructionCHECKCAST theINS = (BytecodeInstructionCHECKCAST) theInstruction;
                Value theValue = aHelper.peek();
                aTargetBlock.getExpressions().add(new CheckCastExpression(theValue, theINS.getTypeCheck()));
            } else if (theInstruction instanceof BytecodeInstructionPOP) {
                BytecodeInstructionPOP theINS = (BytecodeInstructionPOP) theInstruction;
                aHelper.pop();
            } else if (theInstruction instanceof BytecodeInstructionPOP2) {
                BytecodeInstructionPOP2 theINS = (BytecodeInstructionPOP2) theInstruction;
                Value theValue = aHelper.pop();
                switch (theValue.resolveType().resolve()) {
                case LONG:
                    break;
                case DOUBLE:
                    break;
                default:
                    aHelper.pop();
                }
            } else if (theInstruction instanceof BytecodeInstructionDUP) {
                BytecodeInstructionDUP theINS = (BytecodeInstructionDUP) theInstruction;
                Value theValue = aHelper.peek();
                aHelper.push(theValue);
            } else if (theInstruction instanceof BytecodeInstructionDUP2) {
                BytecodeInstructionDUP2 theINS = (BytecodeInstructionDUP2) theInstruction;
                Value theValue1 = aHelper.pop();
                if (theValue1.resolveType().resolve() == TypeRef.Native.LONG || theValue1.resolveType().resolve() == TypeRef.Native.DOUBLE) {
                    // Category 2
                    aHelper.push(theValue1);
                    aHelper.push(theValue1);
                } else {
                    // Category 1
                    Value theValue2 = aHelper.pop();
                    aHelper.push(theValue2);
                    aHelper.push(theValue1);
                    aHelper.push(theValue2);
                    aHelper.push(theValue1);
                }
            } else if (theInstruction instanceof BytecodeInstructionDUP2X1) {
                BytecodeInstructionDUP2X1 theINS = (BytecodeInstructionDUP2X1) theInstruction;
                Value theValue1 = aHelper.pop();
                if (theValue1.resolveType().resolve() == TypeRef.Native.LONG || theValue1.resolveType().resolve() == TypeRef.Native.DOUBLE) {
                    Value theValue2 = aHelper.pop();

                    aHelper.push(theValue1);
                    aHelper.push(theValue2);
                    aHelper.push(theValue2);
                } else {
                    Value theValue2 = aHelper.pop();
                    Value theValue3 = aHelper.pop();

                    aHelper.push(theValue2);
                    aHelper.push(theValue1);
                    aHelper.push(theValue3);
                    aHelper.push(theValue2);
                    aHelper.push(theValue2);
                }
            } else if (theInstruction instanceof BytecodeInstructionDUPX1) {
                BytecodeInstructionDUPX1 theINS = (BytecodeInstructionDUPX1) theInstruction;
                Value theValue1 = aHelper.pop();
                Value theValue2 = aHelper.pop();

                aHelper.push(theValue1);
                aHelper.push(theValue2);
                aHelper.push(theValue1);

            } else if (theInstruction instanceof BytecodeInstructionDUPX2) {
                BytecodeInstructionDUPX2 theINS = (BytecodeInstructionDUPX2) theInstruction;
                Value theValue1 = aHelper.pop();
                Value theValue2 = aHelper.pop();

                if (theValue2.resolveType().resolve() == TypeRef.Native.LONG || theValue2.resolveType().resolve() == TypeRef.Native.DOUBLE) {
                    // Form 2
                    aHelper.push(theValue1);
                    aHelper.push(theValue2);
                    aHelper.push(theValue1);
                } else {
                    // Form 1
                    Value theValue3 = aHelper.pop();

                    aHelper.push(theValue1);
                    aHelper.push(theValue3);
                    aHelper.push(theValue2);
                    aHelper.push(theValue1);
                }
            } else if (theInstruction instanceof BytecodeInstructionGETSTATIC) {
                BytecodeInstructionGETSTATIC theINS = (BytecodeInstructionGETSTATIC) theInstruction;
                GetStaticExpression theValue = new GetStaticExpression(theINS.getConstant());
                Variable theVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getConstant().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType()), theValue);
                aHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionASTORE) {
                BytecodeInstructionASTORE theINS = (BytecodeInstructionASTORE) theInstruction;
                Value theValue = aHelper.pop();
                aHelper.setLocalVariable(theInstruction.getOpcodeAddress(), theINS.getVariableIndex(), theValue);
            } else if (theInstruction instanceof BytecodeInstructionGenericSTORE) {
                BytecodeInstructionGenericSTORE theINS = (BytecodeInstructionGenericSTORE) theInstruction;
                Value theValue = aHelper.pop();
                Variable theOtherVariable = aTargetBlock.newVariable(theValue.resolveType().resolve(), theValue);
                aHelper.setLocalVariable(theInstruction.getOpcodeAddress(), theINS.getVariableIndex(), theOtherVariable);
            } else if (theInstruction instanceof BytecodeInstructionObjectArrayLOAD) {
                BytecodeInstructionObjectArrayLOAD theINS = (BytecodeInstructionObjectArrayLOAD) theInstruction;
                Value theIndex = aHelper.pop();
                Value theTarget = aHelper.pop();
                TypeRef theType = theTarget.resolveType();
                if (theType instanceof TypeRef.ArrayTypeRef) {
                    TypeRef.ArrayTypeRef theArrayTypeRef = (TypeRef.ArrayTypeRef) theTarget.resolveType();
                    Variable theVariable = aTargetBlock.newVariable(
                            TypeRef.toType(theArrayTypeRef.arrayType()), new ArrayEntryExpression(TypeRef.Native.REFERENCE, theTarget, theIndex));
                    aHelper.push(theVariable);
                } else {
                    Variable theVariable = aTargetBlock.newVariable(
                            TypeRef.Native.REFERENCE, new ArrayEntryExpression(TypeRef.Native.REFERENCE, theTarget, theIndex));
                    aHelper.push(theVariable);
                }
            } else if (theInstruction instanceof BytecodeInstructionGenericArrayLOAD) {
                BytecodeInstructionGenericArrayLOAD theINS = (BytecodeInstructionGenericArrayLOAD) theInstruction;
                Value theIndex = aHelper.pop();
                Value theTarget = aHelper.pop();

                Variable theVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new ArrayEntryExpression(TypeRef.toType(theINS.getType()), theTarget, theIndex));
                aHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericArraySTORE) {
                BytecodeInstructionGenericArraySTORE theINS = (BytecodeInstructionGenericArraySTORE) theInstruction;
                Value theValue = aHelper.pop();
                Value theIndex = aHelper.pop();
                Value theTarget = aHelper.pop();
                aTargetBlock.getExpressions().add(new ArrayStoreExpression(TypeRef.toType(theINS.getType()), theTarget, theIndex, theValue));
            } else if (theInstruction instanceof BytecodeInstructionObjectArraySTORE) {
                BytecodeInstructionObjectArraySTORE theINS = (BytecodeInstructionObjectArraySTORE) theInstruction;
                Value theValue = aHelper.pop();
                Value theIndex = aHelper.pop();
                Value theTarget = aHelper.pop();
                aTargetBlock.getExpressions().add(new ArrayStoreExpression(TypeRef.Native.REFERENCE, theTarget, theIndex, theValue));
            } else if (theInstruction instanceof BytecodeInstructionACONSTNULL) {
                BytecodeInstructionACONSTNULL theINS = (BytecodeInstructionACONSTNULL) theInstruction;
                aHelper.push(new NullValue());
            } else if (theInstruction instanceof BytecodeInstructionPUTFIELD) {
                BytecodeInstructionPUTFIELD theINS = (BytecodeInstructionPUTFIELD) theInstruction;
                Value theValue = aHelper.pop();
                Value theTarget = aHelper.pop();
                aTargetBlock.getExpressions().add(new PutFieldExpression(theINS.getFieldRefConstant(), theTarget, theValue));
            } else if (theInstruction instanceof BytecodeInstructionGETFIELD) {
                BytecodeInstructionGETFIELD theINS = (BytecodeInstructionGETFIELD) theInstruction;
                Value theTarget = aHelper.pop();
                Variable theVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getFieldRefConstant().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().fieldType()), new GetFieldExpression(theINS.getFieldRefConstant(), theTarget));
                aHelper.push(theVariable);
            } else if (theInstruction instanceof BytecodeInstructionPUTSTATIC) {
                BytecodeInstructionPUTSTATIC theINS = (BytecodeInstructionPUTSTATIC) theInstruction;
                Value theValue = aHelper.pop();
                aTargetBlock.getExpressions().add(new PutStaticExpression(theINS.getConstant(), theValue));
            } else if (theInstruction instanceof BytecodeInstructionGenericLDC) {
                BytecodeInstructionGenericLDC theINS = (BytecodeInstructionGenericLDC) theInstruction;
                BytecodeConstant theConstant = theINS.constant();
                if (theConstant instanceof BytecodeDoubleConstant) {
                    BytecodeDoubleConstant theC = (BytecodeDoubleConstant) theConstant;
                    aHelper.push(new DoubleValue(theC.getDoubleValue()));
                } else if (theConstant instanceof BytecodeLongConstant) {
                    BytecodeLongConstant theC = (BytecodeLongConstant) theConstant;
                    aHelper.push(new LongValue(theC.getLongValue()));
                } else if (theConstant instanceof BytecodeFloatConstant) {
                    BytecodeFloatConstant theC = (BytecodeFloatConstant) theConstant;
                    aHelper.push(new FloatValue(theC.getFloatValue()));
                } else if (theConstant instanceof BytecodeIntegerConstant) {
                    BytecodeIntegerConstant theC = (BytecodeIntegerConstant) theConstant;
                    aHelper.push(new IntegerValue(theC.getIntegerValue()));
                } else if (theConstant instanceof BytecodeStringConstant) {
                    BytecodeStringConstant theC = (BytecodeStringConstant) theConstant;
                    Variable theVariable = aTargetBlock.newVariable(TypeRef.toType(BytecodeObjectTypeRef.fromRuntimeClass(String.class)), new StringValue(theC.getValue().stringValue()));
                    aHelper.push(theVariable);
                } else if (theConstant instanceof BytecodeClassinfoConstant) {
                    BytecodeClassinfoConstant theC = (BytecodeClassinfoConstant) theConstant;
                    aHelper.push(new ClassReferenceValue(BytecodeObjectTypeRef.fromUtf8Constant(theC.getConstant())));
                } else {
                    throw new IllegalArgumentException("Unsupported constant type : " + theConstant);
                }
            } else if (theInstruction instanceof BytecodeInstructionBIPUSH) {
                BytecodeInstructionBIPUSH theINS = (BytecodeInstructionBIPUSH) theInstruction;
                aHelper.push(new IntegerValue(theINS.getByteValue()));
            } else if (theInstruction instanceof BytecodeInstructionSIPUSH) {
                BytecodeInstructionSIPUSH theINS = (BytecodeInstructionSIPUSH) theInstruction;
                aHelper.push(new IntegerValue(theINS.getShortValue()));
            } else if (theInstruction instanceof BytecodeInstructionICONST) {
                BytecodeInstructionICONST theINS = (BytecodeInstructionICONST) theInstruction;
                aHelper.push(new IntegerValue(theINS.getIntConst()));
            } else if (theInstruction instanceof BytecodeInstructionFCONST) {
                BytecodeInstructionFCONST theINS = (BytecodeInstructionFCONST) theInstruction;
                aHelper.push(new FloatValue(theINS.getFloatValue()));
            } else if (theInstruction instanceof BytecodeInstructionDCONST) {
                BytecodeInstructionDCONST theINS = (BytecodeInstructionDCONST) theInstruction;
                aHelper.push(new DoubleValue(theINS.getDoubleConst()));
            } else if (theInstruction instanceof BytecodeInstructionLCONST) {
                BytecodeInstructionLCONST theINS = (BytecodeInstructionLCONST) theInstruction;
                aHelper.push(new LongValue(theINS.getLongConst()));
            } else if (theInstruction instanceof BytecodeInstructionGenericNEG) {
                BytecodeInstructionGenericNEG theINS = (BytecodeInstructionGenericNEG) theInstruction;
                Value theValue = aHelper.pop();
                Variable theNegatedValue = aTargetBlock.newVariable(theValue.resolveType(), new NegatedExpression(theValue));
                aHelper.push(theNegatedValue);
            } else if (theInstruction instanceof BytecodeInstructionARRAYLENGTH) {
                BytecodeInstructionARRAYLENGTH theINS = (BytecodeInstructionARRAYLENGTH) theInstruction;
                Value theValue = aHelper.pop();
                Variable theNegatedValue = aTargetBlock.newVariable(TypeRef.Native.INT, new ArrayLengthExpression(theValue));
                aHelper.push(theNegatedValue);
            } else if (theInstruction instanceof BytecodeInstructionGenericLOAD) {
                BytecodeInstructionGenericLOAD theINS = (BytecodeInstructionGenericLOAD) theInstruction;
                Value theValue = aHelper.getLocalVariable(theINS.getVariableIndex());
                aHelper.push(theValue);
            } else if (theInstruction instanceof BytecodeInstructionALOAD) {
                BytecodeInstructionALOAD theINS = (BytecodeInstructionALOAD) theInstruction;
                Value theValue = aHelper.getLocalVariable(theINS.getVariableIndex());
                aHelper.push(theValue);
            } else if (theInstruction instanceof BytecodeInstructionGenericCMP) {
                BytecodeInstructionGenericCMP theINS = (BytecodeInstructionGenericCMP) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT, new CompareExpression(theValue1, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionLCMP) {
                BytecodeInstructionLCMP theINS = (BytecodeInstructionLCMP) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT, new CompareExpression(theValue1, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionIINC) {
                BytecodeInstructionIINC theINS = (BytecodeInstructionIINC) theInstruction;
                Value theValueToIncrement = aHelper.getLocalVariable(theINS.getIndex());
                Value theNewVariable = aTargetBlock.newVariable(
                        TypeRef.Native.INT, new BinaryExpression(TypeRef.Native.INT, theValueToIncrement, BinaryExpression.Operator.ADD, new IntegerValue(theINS.getConstant())));
                aHelper.setLocalVariable(theInstruction.getOpcodeAddress(), theINS.getIndex(), theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericREM) {
                BytecodeInstructionGenericREM theINS = (BytecodeInstructionGenericREM) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryExpression(TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.REMAINDER, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericADD) {
                BytecodeInstructionGenericADD theINS = (BytecodeInstructionGenericADD) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryExpression(TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.ADD, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericDIV) {
                BytecodeInstructionGenericDIV theINS = (BytecodeInstructionGenericDIV) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();

                Variable theNewVariable;
                Value theDivValue = new BinaryExpression(TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.DIV, theValue2);
                switch (theINS.getType()) {
                case FLOAT:
                    theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), theDivValue);
                    break;
                case DOUBLE:
                    theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), theDivValue);
                    break;
                default:
                    theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new FloorExpression(theDivValue, TypeRef.toType(theINS.getType())));
                    break;
                }

                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericMUL) {
                BytecodeInstructionGenericMUL theINS = (BytecodeInstructionGenericMUL) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryExpression(TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.MUL, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSUB) {
                BytecodeInstructionGenericSUB theINS = (BytecodeInstructionGenericSUB) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryExpression(TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.SUB, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericXOR) {
                BytecodeInstructionGenericXOR theINS = (BytecodeInstructionGenericXOR) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryExpression(TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.BINARYXOR, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericOR) {
                BytecodeInstructionGenericOR theINS = (BytecodeInstructionGenericOR) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryExpression(TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.BINARYOR, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericAND) {
                BytecodeInstructionGenericAND theINS = (BytecodeInstructionGenericAND) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryExpression(TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.BINARYAND, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSHL) {
                BytecodeInstructionGenericSHL theINS = (BytecodeInstructionGenericSHL) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryExpression(TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.BINARYSHIFTLEFT, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericSHR) {
                BytecodeInstructionGenericSHR theINS = (BytecodeInstructionGenericSHR) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryExpression(TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.BINARYSHIFTRIGHT, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGenericUSHR) {
                BytecodeInstructionGenericUSHR theINS = (BytecodeInstructionGenericUSHR) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getType()), new BinaryExpression(TypeRef.toType(theINS.getType()), theValue1, BinaryExpression.Operator.BINARYUNSIGNEDSHIFTRIGHT, theValue2));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionIFNULL) {
                BytecodeInstructionIFNULL theINS = (BytecodeInstructionIFNULL) theInstruction;
                Value theValue = aHelper.pop();
                FixedBinaryExpression theBinaryValue = new FixedBinaryExpression(theValue, FixedBinaryExpression.Operator.ISNULL);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget()));

                aTargetBlock.getExpressions().add(new IFExpression(theINS.getOpcodeAddress(), theINS.getJumpTarget(), theBinaryValue, theExpressions));
            } else if (theInstruction instanceof BytecodeInstructionIFNONNULL) {
                BytecodeInstructionIFNONNULL theINS = (BytecodeInstructionIFNONNULL) theInstruction;
                Value theValue = aHelper.pop();
                FixedBinaryExpression theBinaryValue = new FixedBinaryExpression(theValue, FixedBinaryExpression.Operator.ISNONNULL);

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget()));

                aTargetBlock.getExpressions().add(new IFExpression(theINS.getOpcodeAddress(), theINS.getJumpTarget(), theBinaryValue, theExpressions));
            } else if (theInstruction instanceof BytecodeInstructionIFICMP) {
                BytecodeInstructionIFICMP theINS = (BytecodeInstructionIFICMP) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                BinaryExpression theBinaryValue;
                switch (theINS.getType()) {
                case lt:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.LESSTHAN, theValue2);
                    break;
                case eq:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.EQUALS, theValue2);
                    break;
                case gt:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.GREATERTHAN, theValue2);
                    break;
                case ge:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.GREATEROREQUALS, theValue2);
                    break;
                case le:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.LESSTHANOREQUALS, theValue2);
                    break;
                case ne:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.NOTEQUALS, theValue2);
                    break;
                default:
                    throw new IllegalStateException("Not supported operation : " + theINS.getType());
                }

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget()));

                aTargetBlock.getExpressions().add(new IFExpression(theINS.getOpcodeAddress(), theINS.getJumpTarget(), theBinaryValue, theExpressions));

            } else if (theInstruction instanceof BytecodeInstructionIFACMP) {
                BytecodeInstructionIFACMP theINS = (BytecodeInstructionIFACMP) theInstruction;
                Value theValue2 = aHelper.pop();
                Value theValue1 = aHelper.pop();
                BinaryExpression theBinaryValue;
                switch (theINS.getType()) {
                case eq:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.EQUALS, theValue2);
                    break;
                case ne:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue1, BinaryExpression.Operator.NOTEQUALS, theValue2);
                    break;
                default:
                    throw new IllegalStateException("Not supported operation : " + theINS.getType());
                }

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget()));

                aTargetBlock.getExpressions().add(new IFExpression(theINS.getOpcodeAddress(), theINS.getJumpTarget(), theBinaryValue, theExpressions));

            } else if (theInstruction instanceof BytecodeInstructionIFCOND) {
                BytecodeInstructionIFCOND theINS = (BytecodeInstructionIFCOND) theInstruction;
                Value theValue = aHelper.pop();
                BinaryExpression theBinaryValue;
                switch (theINS.getType()) {
                case lt:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue, BinaryExpression.Operator.LESSTHAN, new IntegerValue(0));
                    break;
                case eq:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue, BinaryExpression.Operator.EQUALS, new IntegerValue(0));
                    break;
                case gt:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue, BinaryExpression.Operator.GREATERTHAN, new IntegerValue(0));
                    break;
                case ge:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue, BinaryExpression.Operator.GREATEROREQUALS, new IntegerValue(0));
                    break;
                case le:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue, BinaryExpression.Operator.LESSTHANOREQUALS, new IntegerValue(0));
                    break;
                case ne:
                    theBinaryValue = new BinaryExpression(TypeRef.Native.BOOLEAN, theValue, BinaryExpression.Operator.NOTEQUALS, new IntegerValue(0));
                    break;
                default:
                    throw new IllegalStateException("Not supported operation : " + theINS.getType());
                }

                ExpressionList theExpressions = new ExpressionList();
                theExpressions.add(new GotoExpression(theINS.getJumpTarget()));

                aTargetBlock.getExpressions().add(new IFExpression(theINS.getOpcodeAddress(), theINS.getJumpTarget(), theBinaryValue, theExpressions));
            } else if (theInstruction instanceof BytecodeInstructionObjectRETURN) {
                BytecodeInstructionObjectRETURN theINS = (BytecodeInstructionObjectRETURN) theInstruction;
                Value theValue = aHelper.pop();
                aTargetBlock.getExpressions().add(new ReturnValueExpression(theValue));
            } else if (theInstruction instanceof BytecodeInstructionGenericRETURN) {
                BytecodeInstructionGenericRETURN theINS = (BytecodeInstructionGenericRETURN) theInstruction;
                Value theValue = aHelper.pop();
                aTargetBlock.getExpressions().add(new ReturnValueExpression(theValue));
            } else if (theInstruction instanceof BytecodeInstructionATHROW) {
                BytecodeInstructionATHROW theINS = (BytecodeInstructionATHROW) theInstruction;
                Value theValue = aHelper.pop();
                aTargetBlock.getExpressions().add(new ThrowExpression(theValue));
            } else if (theInstruction instanceof BytecodeInstructionRETURN) {
                BytecodeInstructionRETURN theINS = (BytecodeInstructionRETURN) theInstruction;
                aTargetBlock.getExpressions().add(new ReturnExpression());
            } else if (theInstruction instanceof BytecodeInstructionNEW) {
                BytecodeInstructionNEW theINS = (BytecodeInstructionNEW) theInstruction;

                BytecodeClassinfoConstant theClassInfo = theINS.getClassInfoForObjectToCreate();
                BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(theClassInfo.getConstant());
                if (Objects.equals(theObjectType.name(), Address.class.getName())) {
                    // At this time the exact location is unknown, the value
                    // will be set at constructor invocation time
                    Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT);
                    aHelper.push(theNewVariable);
                } else {
                    if (Objects.equals(theObjectType, BytecodeObjectTypeRef.fromRuntimeClass(TRuntimeGeneratedType.class))) {
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.REFERENCE, new RuntimeGeneratedTypeExpression());
                        aHelper.push(theNewVariable);
                    } else {
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theObjectType), new NewObjectExpression(theClassInfo));
                        aHelper.push(theNewVariable);
                    }
                }
            } else if (theInstruction instanceof BytecodeInstructionNEWARRAY) {
                BytecodeInstructionNEWARRAY theINS = (BytecodeInstructionNEWARRAY) theInstruction;
                Value theLength = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(
                        TypeRef.Native.REFERENCE, new NewArrayExpression(theINS.getPrimitiveType(), theLength));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionNEWMULTIARRAY) {
                BytecodeInstructionNEWMULTIARRAY theINS = (BytecodeInstructionNEWMULTIARRAY) theInstruction;
                List<Value> theDimensions = new ArrayList<>();
                for (int i=0;i<theINS.getDimensions();i++) {
                    theDimensions.add(aHelper.pop());
                }
                Collections.reverse(theDimensions);
                BytecodeTypeRef theTypeRef = linkerContext.getSignatureParser().toFieldType(new BytecodeUtf8Constant(theINS.getTypeConstant().getConstant().stringValue()));
                Variable theNewVariable = aTargetBlock.newVariable(
                        TypeRef.Native.REFERENCE, new NewMultiArrayExpression(theTypeRef, theDimensions));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionANEWARRAY) {
                BytecodeInstructionANEWARRAY theINS = (BytecodeInstructionANEWARRAY) theInstruction;
                Value theLength = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(
                        TypeRef.Native.REFERENCE, new NewArrayExpression(theINS.getObjectType(), theLength));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionGOTO) {
                BytecodeInstructionGOTO theINS = (BytecodeInstructionGOTO) theInstruction;
                aTargetBlock.getExpressions().add(new GotoExpression(theINS.getJumpAddress()));
            } else if (theInstruction instanceof BytecodeInstructionL2Generic) {
                BytecodeInstructionL2Generic theINS = (BytecodeInstructionL2Generic) theInstruction;
                Value theValue = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getTargetType()), new TypeConversionExpression(theValue, TypeRef
                        .toType(theINS.getTargetType())));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionI2Generic) {
                BytecodeInstructionI2Generic theINS = (BytecodeInstructionI2Generic) theInstruction;
                Value theValue = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getTargetType()), new TypeConversionExpression(theValue, TypeRef
                        .toType(theINS.getTargetType())));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionF2Generic) {
                BytecodeInstructionF2Generic theINS = (BytecodeInstructionF2Generic) theInstruction;
                Value theValue = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getTargetType()), new TypeConversionExpression(theValue, TypeRef
                        .toType(theINS.getTargetType())));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionD2Generic) {
                BytecodeInstructionD2Generic theINS = (BytecodeInstructionD2Generic) theInstruction;
                Value theValue = aHelper.pop();
                Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theINS.getTargetType()), new TypeConversionExpression(theValue, TypeRef
                        .toType(theINS.getTargetType())));
                aHelper.push(theNewVariable);
            } else if (theInstruction instanceof BytecodeInstructionINVOKESPECIAL) {
                BytecodeInstructionINVOKESPECIAL theINS = (BytecodeInstructionINVOKESPECIAL) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Value> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(aHelper.pop());
                }
                Collections.reverse(theArguments);

                Variable theTarget = (Variable) aHelper.pop();
                BytecodeObjectTypeRef theType = BytecodeObjectTypeRef.fromUtf8Constant(theINS.getMethodReference().getClassIndex().getClassConstant().getConstant());
                if (Objects.equals(theType, BytecodeObjectTypeRef.fromRuntimeClass(TRuntimeGeneratedType.class))) {
                    RuntimeGeneratedTypeExpression theValue = (RuntimeGeneratedTypeExpression) theTarget.incomingDataFlows().get(0);
                    theValue.setType(theArguments.get(0));
                    theValue.setMethodRef(theArguments.get(1));
                } else if (Objects.equals(theType, BytecodeObjectTypeRef.fromRuntimeClass(Address.class))) {
                    theTarget.initializeWith(theArguments.get(0));
                    aTargetBlock.getExpressions().add(new VariableAssignmentExpression(theTarget, theArguments.get(0)));
                } else {
                    String theMethodName = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue();
                    if ("getClass".equals(theMethodName) && BytecodeLinkedClass.GET_CLASS_SIGNATURE.metchesExactlyTo(theSignature)) {
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theSignature.getReturnType()), new TypeOfExpression(theTarget));
                        aHelper.push(theNewVariable);
                    } else {
                        DirectInvokeMethodExpression theExpression = new DirectInvokeMethodExpression(theType, theMethodName, theSignature, theTarget, theArguments);
                        if (theSignature.getReturnType().isVoid()) {
                            aTargetBlock.getExpressions().add(theExpression);
                        } else {
                            Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theSignature.getReturnType()), theExpression);
                            aHelper.push(theNewVariable);
                        }
                    }
                }
            } else if (theInstruction instanceof BytecodeInstructionINVOKEVIRTUAL) {
                BytecodeInstructionINVOKEVIRTUAL theINS = (BytecodeInstructionINVOKEVIRTUAL) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                if (theSignature.metchesExactlyTo(BytecodeLinkedClass.GET_CLASS_SIGNATURE) && "getClass".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())) {
                    Value theValue = new TypeOfExpression(aHelper.pop());
                    Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theSignature.getReturnType()), theValue);
                    aHelper.push(theNewVariable);
                    continue;
                }

                List<Value> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(aHelper.pop());
                }
                Collections.reverse(theArguments);

                Value theTarget = aHelper.pop();
                InvokeVirtualMethodExpression theExpression = new InvokeVirtualMethodExpression(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType(), theTarget, theArguments);
                if (theSignature.getReturnType().isVoid()) {
                    aTargetBlock.getExpressions().add(theExpression);
                } else {
                    Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theSignature.getReturnType()), theExpression);
                    aHelper.push(theNewVariable);
                }
            } else if (theInstruction instanceof BytecodeInstructionINVOKEINTERFACE) {
                BytecodeInstructionINVOKEINTERFACE theINS = (BytecodeInstructionINVOKEINTERFACE) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodDescriptor().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Value> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(aHelper.pop());
                }
                Collections.reverse(theArguments);

                Value theTarget = aHelper.pop();
                InvokeVirtualMethodExpression theExpression = new InvokeVirtualMethodExpression(theINS.getMethodDescriptor().getNameAndTypeIndex().getNameAndType(), theTarget, theArguments);
                if (theSignature.getReturnType().isVoid()) {
                    aTargetBlock.getExpressions().add(theExpression);
                } else {
                    Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theSignature.getReturnType()), theExpression);
                    aHelper.push(theNewVariable);
                }

            } else if (theInstruction instanceof BytecodeInstructionINVOKESTATIC) {
                BytecodeInstructionINVOKESTATIC theINS = (BytecodeInstructionINVOKESTATIC) theInstruction;
                BytecodeMethodSignature theSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                List<Value> theArguments = new ArrayList<>();
                BytecodeTypeRef[] theArgumentTypes = theSignature.getArguments();
                for (BytecodeTypeRef theArgumentType : theArgumentTypes) {
                    theArguments.add(aHelper.pop());
                }
                Collections.reverse(theArguments);

                BytecodeClassinfoConstant theTargetClass = theINS.getMethodReference().getClassIndex().getClassConstant();
                BytecodeObjectTypeRef theObjectType = BytecodeObjectTypeRef.fromUtf8Constant(theTargetClass.getConstant());
                if (Objects.equals(theObjectType.name(), MemoryManager.class.getName()) && "initTestMemory".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())) {
                    // This invocation can be skipped!!!
                } else if (Objects.equals(theObjectType.name(), Address.class.getName())) {
                    String theMethodName = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex()
                            .getName().stringValue();
                    switch (theMethodName) {
                    case "setIntValue": {

                        Value theTarget = theArguments.get(0);
                        Value theOffset = theArguments.get(1);
                        Value theNewValue = theArguments.get(2);

                        ComputedMemoryLocationWriteExpression theLocation = new ComputedMemoryLocationWriteExpression(theTarget, theOffset);
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT, theLocation);
                        aTargetBlock.getExpressions().add(new SetMemoryLocationExpression(theNewVariable, theNewValue));
                        break;
                    }
                    case "getStart": {

                        Value theTarget = theArguments.get(0);
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT, theTarget);

                        aHelper.push(theNewVariable);
                        break;
                    }
                    case "getStackTop": {

                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT, new StackTopExpression());

                        aHelper.push(theNewVariable);
                        break;
                    }
                    case "getMemorySize": {

                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT, new MemorySizeExpression());

                        aHelper.push(theNewVariable);
                        break;
                    }
                    case "getIntValue": {

                        Value theTarget = theArguments.get(0);
                        Value theOffset = theArguments.get(1);

                        ComputedMemoryLocationReadExpression theLocation = new ComputedMemoryLocationReadExpression(theTarget, theOffset);
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.Native.INT, theLocation);
                        aHelper.push(theNewVariable);

                        break;
                    }
                    case "unreachable": {
                        aTargetBlock.getExpressions().add(new UnreachableExpression());
                        break;
                    }
                    default:
                        throw new IllegalStateException("Not implemented : " + theMethodName);
                    }
                } else {
                    BytecodeObjectTypeRef theClassToInvoke = BytecodeObjectTypeRef.fromUtf8Constant(theINS.getMethodReference().getClassIndex().getClassConstant().getConstant());
                    linkerContext.resolveClass(theClassToInvoke)
                            .resolveStaticMethod(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                    theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature());

                    BytecodeMethodSignature theCalledSignature = theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();

                    if ("sqrt".equals(theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())
                            && "de.mirkosertic.bytecoder.classlib.java.lang.TStrictMath".equals(theClassToInvoke.name())) {
                        Value theValue = new SqrtExpression(TypeRef.toType(theCalledSignature.getReturnType()), theArguments.get(0));
                        Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theSignature.getReturnType()), theValue);
                        aHelper.push(theNewVariable);
                    } else {
                        InvokeStaticMethodExpression theExpression = new InvokeStaticMethodExpression(
                                theClassToInvoke,
                                theINS.getMethodReference().getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                                theCalledSignature,
                                theArguments);
                        if (theSignature.getReturnType().isVoid()) {
                            aTargetBlock.getExpressions().add(theExpression);
                        } else {
                            Variable theNewVariable = aTargetBlock.newVariable(TypeRef.toType(theSignature.getReturnType()), theExpression);
                            aHelper.push(theNewVariable);
                        }
                    }
                }
            } else if (theInstruction instanceof BytecodeInstructionINSTANCEOF) {
                BytecodeInstructionINSTANCEOF theINS = (BytecodeInstructionINSTANCEOF) theInstruction;

                Value theValueToCheck = aHelper.pop();
                InstanceOfExpression theValue = new InstanceOfExpression(theValueToCheck, theINS.getTypeRef());

                Variable theCheckResult = aTargetBlock.newVariable(TypeRef.Native.BOOLEAN, theValue);
                aHelper.push(theCheckResult);
            } else if (theInstruction instanceof BytecodeInstructionTABLESWITCH) {
                BytecodeInstructionTABLESWITCH theINS = (BytecodeInstructionTABLESWITCH) theInstruction;
                Value theValue = aHelper.pop();

                ExpressionList theDefault = new ExpressionList();
                theDefault.add(new GotoExpression(theINS.getDefaultJumpTarget()));

                Map<Long, ExpressionList> theOffsets = new HashMap<>();
                long[] theJumpTargets = theINS.getOffsets();
                for (int i=0;i<theJumpTargets.length;i++) {
                    ExpressionList theJump = new ExpressionList();
                    theJump.add(new GotoExpression(theINS.getOpcodeAddress().add((int) theJumpTargets[i])));
                    theOffsets.put((long) i, theJump);
                }

                aTargetBlock.getExpressions().add(new TableSwitchExpression(theValue, theINS.getLowValue(), theINS.getHighValue(),
                        theDefault, theOffsets));
            } else if (theInstruction instanceof BytecodeInstructionLOOKUPSWITCH) {
                BytecodeInstructionLOOKUPSWITCH theINS = (BytecodeInstructionLOOKUPSWITCH) theInstruction;
                Value theValue = aHelper.pop();

                ExpressionList theDefault = new ExpressionList();
                theDefault.add(new GotoExpression(theINS.getDefaultJumpTarget()));

                Map<Long, ExpressionList> thePairs = new HashMap<>();
                for (BytecodeInstructionLOOKUPSWITCH.Pair thePair : theINS.getPairs()) {
                    ExpressionList thePairExpressions = new ExpressionList();
                    thePairExpressions.add(new GotoExpression(theINS.getOpcodeAddress().add((int) thePair.getOffset())));
                    thePairs.put(thePair.getMatch(), thePairExpressions);
                }

                aTargetBlock.getExpressions().add(new LookupSwitchExpression(theValue, theDefault, thePairs));
            } else if (theInstruction instanceof BytecodeInstructionINVOKEDYNAMIC) {
                BytecodeInstructionINVOKEDYNAMIC theINS = (BytecodeInstructionINVOKEDYNAMIC) theInstruction;

                BytecodeInvokeDynamicConstant theConstant = theINS.getCallSite();
                BytecodeMethodSignature theInitSignature = theConstant.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature();


                BytecodeBootstrapMethodsAttributeInfo theBootStrapMethods = aOwningClass.getAttributes().getByType(BytecodeBootstrapMethodsAttributeInfo.class);
                BytecodeBootstrapMethod theBootstrapMethod = theBootStrapMethods.methodByIndex(theConstant.getBootstrapMethodAttributeIndex().getIndex());

                BytecodeMethodHandleConstant theMethodRef = theBootstrapMethod.getMethodRef();
                BytecodeMethodRefConstant theBootstrapMethodToInvoke = (BytecodeMethodRefConstant) theMethodRef.getReferenceIndex().getConstant();

                Program theProgram = new Program();
                RegionNode theInitNode = theProgram.getControlFlowGraph().createAt(BytecodeOpcodeAddress.START_AT_ZERO, RegionNode.BlockType.NORMAL);

                switch (theMethodRef.getReferenceKind()) {
                case REF_invokeStatic: {

                    BytecodeObjectTypeRef theClassWithBootstrapMethod = BytecodeObjectTypeRef
                            .fromUtf8Constant(theBootstrapMethodToInvoke.getClassIndex().getClassConstant().getConstant());

                    BytecodeMethodSignature theSignature = theBootstrapMethodToInvoke.getNameAndTypeIndex().getNameAndType()
                            .getDescriptorIndex().methodSignature();

                    List<Value> theArguments = new ArrayList<>();
                    // Add the three default constants
                    // TMethodHandles.Lookup aCaller,
                    theArguments.add(theInitNode
                            .newVariable(TypeRef.Native.REFERENCE, new MethodHandlesGeneratedLookupExpression(theClassWithBootstrapMethod)));
                    theArguments.add(theInitNode.newVariable(
                            TypeRef.Native.REFERENCE, new StringValue(theConstant.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue())));
                    // TMethodType aInvokedType,
                    theArguments.add(theInitNode.newVariable(TypeRef.Native.REFERENCE, new MethodTypeExpression(
                            theInitSignature)));

                    // Revolve the static arguments
                    for (BytecodeConstant theArgumentConstant : theBootstrapMethod.getArguments()) {

                        if (theArgumentConstant instanceof BytecodeMethodTypeConstant) {
                            BytecodeMethodTypeConstant theMethodType = (BytecodeMethodTypeConstant) theArgumentConstant;
                            theArguments.add(theInitNode.newVariable(TypeRef.Native.REFERENCE,
                                    new MethodTypeExpression(theMethodType.getDescriptorIndex().methodSignature())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeStringConstant) {
                            BytecodeStringConstant thePrimitive = (BytecodeStringConstant) theArgumentConstant;
                            theArguments.add(theInitNode
                                    .newVariable(TypeRef.Native.REFERENCE, new StringValue(thePrimitive.getValue().stringValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeLongConstant) {
                            BytecodeLongConstant thePrimitive = (BytecodeLongConstant) theArgumentConstant;
                            theArguments.add(theInitNode.newVariable(TypeRef.Native.LONG, new LongValue(thePrimitive.getLongValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeIntegerConstant) {
                            BytecodeIntegerConstant thePrimitive = (BytecodeIntegerConstant) theArgumentConstant;
                            theArguments.add(theInitNode.newVariable(TypeRef.Native.INT, new LongValue(thePrimitive.getIntegerValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeFloatConstant) {
                            BytecodeFloatConstant thePrimitive = (BytecodeFloatConstant) theArgumentConstant;
                            theArguments.add(theInitNode.newVariable(TypeRef.Native.FLOAT, new FloatValue(thePrimitive.getFloatValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeDoubleConstant) {
                            BytecodeDoubleConstant thePrimitive = (BytecodeDoubleConstant) theArgumentConstant;
                            theArguments
                            .add(theInitNode.newVariable(TypeRef.Native.DOUBLE, new DoubleValue(thePrimitive.getDoubleValue())));
                            continue;
                        }
                        if (theArgumentConstant instanceof BytecodeMethodHandleConstant) {
                            BytecodeMethodHandleConstant theMethodHandle = (BytecodeMethodHandleConstant) theArgumentConstant;
                            BytecodeReferenceIndex theReference = theMethodHandle.getReferenceIndex();
                            BytecodeMethodRefConstant theReferenceConstant = (BytecodeMethodRefConstant) theReference
                                    .getConstant();
                            theArguments.add(theInitNode.newVariable(TypeRef.Native.REFERENCE, new MethodRefExpression(theReferenceConstant)));
                            continue;
                        }
                        throw new IllegalStateException("Unsupported argument type : " + theArgumentConstant);
                    }

                    // Ok, is the last argument of the bootstrap method a vararg argument
                    BytecodeTypeRef theLastArgument = theSignature.getArguments()[theSignature.getArguments().length - 1];
                    if (theLastArgument.isArray()) {
                        // Yes, so we have to wrap everything from this position on in an array
                        int theSignatureLength = theSignature.getArguments().length;
                        int theArgumentsLength = theArguments.size();

                        int theVarArgsLength = theArgumentsLength - theSignatureLength + 1;
                        Variable theNewVarargsArray = theInitNode.newVariable(TypeRef.Native.REFERENCE, new NewArrayExpression(
                                BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new IntegerValue(theVarArgsLength)));
                        for (int i = theSignatureLength - 1; i < theArgumentsLength; i++) {
                            Value theVariable = theArguments.get(i);
                            theArguments.remove(theVariable);
                            theInitNode.getExpressions().add(new ArrayStoreExpression(TypeRef.Native.REFERENCE, theNewVarargsArray, new IntegerValue(i - theSignatureLength + 1), theVariable));
                        }
                        theArguments.add(theNewVarargsArray);
                    }

                    InvokeStaticMethodExpression theInvokeStaticValue = new InvokeStaticMethodExpression(
                            BytecodeObjectTypeRef.fromUtf8Constant(theBootstrapMethodToInvoke.getClassIndex().getClassConstant().getConstant()),
                            theBootstrapMethodToInvoke.getNameAndTypeIndex().getNameAndType().getNameIndex().getName().stringValue(),
                            theBootstrapMethodToInvoke.getNameAndTypeIndex().getNameAndType().getDescriptorIndex().methodSignature(),
                            theArguments);
                    Variable theNewVariable = theInitNode.newVariable(TypeRef.Native.REFERENCE, theInvokeStaticValue);
                    theInitNode.getExpressions().add(new ReturnValueExpression(theNewVariable));

                    // First step, we construct a callsite
                    ResolveCallsiteObjectExpression theValue = new ResolveCallsiteObjectExpression(aOwningClass.getThisInfo().getConstant().stringValue() + "_" + aMethod.getName().stringValue() + "_" + theINS.getOpcodeAddress().getAddress(), aOwningClass, theProgram, theInitNode);
                    Variable theCallsiteVariable = aTargetBlock.newVariable(TypeRef.Native.REFERENCE, theValue);

                    // Second step, we invoke the callsite to get whatever we are searching
                    InvokeVirtualMethodExpression theGetTargetValue = new InvokeVirtualMethodExpression("getTarget",
                            new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(MethodHandle.class),
                                    new BytecodeTypeRef[0]),
                            theCallsiteVariable, new ArrayList<>());
                    Variable theMethodHandleVariable = aTargetBlock.newVariable(TypeRef.Native.REFERENCE, theGetTargetValue);

                    List<Value> theInvokeArguments = new ArrayList<>();

                    Variable theArray = aTargetBlock.newVariable(
                            TypeRef.Native.REFERENCE, new NewArrayExpression(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), new IntegerValue(theInitSignature.getArguments().length)));

                    for (int i=theInitSignature.getArguments().length-1;i>=0;i--) {
                        Value theIndex = new IntegerValue(i);
                        Value theStoredValue = aHelper.pop();

                        if (theStoredValue.resolveType() == TypeRef.Native.INT) {
                            // Create Integer object to contain int
                            BytecodeObjectTypeRef theType = BytecodeObjectTypeRef.fromRuntimeClass(Integer.class);
                            BytecodeTypeRef[] args_def = new BytecodeTypeRef[]{BytecodePrimitiveTypeRef.INT};
                            BytecodeMethodSignature sig = new BytecodeMethodSignature(theType, args_def);
                            List<Value> args = new ArrayList<>();
                            args.add(theStoredValue);

                            theStoredValue = new InvokeStaticMethodExpression(theType, "valueOf", sig, args);
                            theStoredValue = aTargetBlock.newVariable(TypeRef.Native.REFERENCE, theStoredValue);
                        }

                        aTargetBlock.getExpressions().add(new ArrayStoreExpression(TypeRef.Native.REFERENCE, theArray, theIndex, theStoredValue));
                    }

                    theInvokeArguments.add(theArray);

                    InvokeVirtualMethodExpression theInvokeValue = new InvokeVirtualMethodExpression("invokeExact",
                            new BytecodeMethodSignature(BytecodeObjectTypeRef.fromRuntimeClass(Object.class),
                                    new BytecodeTypeRef[] {
                                            new BytecodeArrayTypeRef(BytecodeObjectTypeRef.fromRuntimeClass(Object.class), 1) }),
                            theMethodHandleVariable, theInvokeArguments);

                    Variable theInvokeExactResult = aTargetBlock.newVariable(TypeRef.Native.REFERENCE, theInvokeValue);
                    aHelper.push(theInvokeExactResult);

                    break;
                }
                default:
                    throw new IllegalStateException(
                            "Nut supported reference kind for invoke dynamic : " + theMethodRef.getReferenceKind());
                }
            } else {
                throw new IllegalArgumentException("Not implemented : " + theInstruction);
            }
        }

        aHelper.finalizeExportState();
    }
}