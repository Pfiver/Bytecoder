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

import de.mirkosertic.bytecoder.core.BytecodeOpcodeAddress;

import java.util.ArrayList;
import java.util.List;

public class ExpressionList {

    private final List<Expression> expressions;

    public ExpressionList() {
        expressions = new ArrayList<>();
    }

    public void add(Expression aExpression) {
        expressions.add(aExpression);
    }

    public List<Expression> toList() {
        return new ArrayList<>(expressions);
    }

    public int size() {
        return expressions.size();
    }

    public Expression lastExpression() {
        if (expressions.isEmpty()) {
            return null;
        }
        int theLastIndex = expressions.size() - 1;
        return expressions.get(theLastIndex);
    }

    public void addBefore(Expression aNewExpression, Expression aTarget) {
        expressions.add(expressions.indexOf(aTarget), aNewExpression);
    }

    public void replace(Expression aExpressionToReplace, Expression aNewExpression) {
        int p = expressions.indexOf(aExpressionToReplace);
        if (p>=0) {
            expressions.remove(p);
            expressions.add(p, aNewExpression);
        }
    }

    public void replace(Expression aExpressionToReplace, ExpressionList aList) {
        int p = expressions.indexOf(aExpressionToReplace);
        if (p>=0) {
            expressions.remove(p);
            List<Expression> theList = aList.toList();
            for (int i = theList.size() - 1; i >= 0; i--) {
                expressions.add(p, theList.get(i));
            }
        }
    }

    public void remove(Expression aExpression) {
        expressions.remove(aExpression);
    }

    public Expression predecessorOf(Expression aExpression) {
        int p = expressions.indexOf(aExpression);
        if (p>0) {
            return expressions.get(p-1);
        }
        return null;
    }

    public boolean endWithNeverReturningExpression() {
        Expression theLastExpression = lastExpression();
        return theLastExpression instanceof ReturnExpression ||
                theLastExpression instanceof ReturnValueExpression ||
                theLastExpression instanceof TableSwitchExpression ||
                theLastExpression instanceof LookupSwitchExpression ||
                theLastExpression instanceof ThrowExpression ||
                theLastExpression instanceof GotoExpression;
    }

    public boolean endsWithReturn() {
        Expression theLastExpression = lastExpression();
        return theLastExpression instanceof ReturnExpression ||
                theLastExpression instanceof ReturnValueExpression;
    }

    public List<BytecodeOpcodeAddress> jumpTargets() {
        List<BytecodeOpcodeAddress> theTargets = new ArrayList<>();
        for (Expression theExpression : expressions) {
            if (theExpression instanceof GotoExpression) {
                GotoExpression theGoto = (GotoExpression) theExpression;
                theTargets.add(theGoto.getJumpTarget());
            }
            if (theExpression instanceof ExpressionListContainer) {
                ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                for (ExpressionList theList : theContainer.getExpressionLists()) {
                    theTargets.addAll(theList.jumpTargets());
                }
            }
        }
        return theTargets;
    }
}
