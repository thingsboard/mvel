/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.ast;

import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import java.util.HashMap;
import java.util.List;

import static org.mvel2.MVEL.eval;
import static org.mvel2.util.CompilerTools.expectType;
import static org.mvel2.util.ParseTools.subCompileExpression;

/**
 * @author Nick
 */
public class SwitchNode extends BlockNode implements NestedStatement {
  protected ExecutableStatement condition;
  protected ExecutableStatement nestedStatement;

  protected SwitchNode switchCase;
  protected ExecutableStatement defaultBlock;

  protected boolean idxAlloc = false;

  protected String conditionSwitchKey;
  protected String conditionOperator = " === ";
  protected int blocSwitchEnd;

  protected  List<String> conditionValues;

  public SwitchNode(char[] expr, int start, int offset, int blockStart, int blockOffset, int fields, ParserContext pCtx,  int blocSwitchEnd, String conditionSwitchKey, List<String> conditionValues) {
    super(pCtx);
    this.expr = expr;
    this.start = start;
    this.offset = offset;
    this.blockStart = blockStart;
    this.blockOffset = blockOffset;
    this.blocSwitchEnd = blocSwitchEnd;
    this.conditionSwitchKey = conditionSwitchKey;
    this.conditionValues = conditionValues;

    idxAlloc = pCtx != null && pCtx.isIndexAllocation();
    //  start ...  offset ->  (...) === (msg.temperature === 19)
    // blockStart ... blockOffset -> after { ... in }

    if ((fields & COMPILE_IMMEDIATE) != 0) {
      String conditionStr = "";
      for (String conditionValue: this.conditionValues) {
        if (conditionStr.isEmpty()) {
          conditionStr = "(" + this.conditionSwitchKey + this.conditionOperator + conditionValue;
        } else {
          conditionStr += (" || " + this.conditionSwitchKey + this.conditionOperator + conditionValue);
        }
      }
      if (!conditionStr.isEmpty()) {
        conditionStr += ")";
//        expectType(pCtx, this.condition = (ExecutableStatement) subCompileExpression(expr, start, offset, pCtx),
        expectType(pCtx, this.condition = (ExecutableStatement) subCompileExpression(conditionStr.toCharArray(), 0, conditionStr.length(), pCtx),
                Boolean.class, true);
      }
      if (pCtx != null) {
        pCtx.pushVariableScope();
      }
      this.nestedStatement = (ExecutableStatement) subCompileExpression(expr, blockStart, blockOffset, pCtx);

      if (pCtx != null) {
        pCtx.popVariableScope();
      }
    }
  }

  public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
    if ((Boolean) condition.getValue(ctx, thisValue, factory)) {
      return nestedStatement.getValue(ctx, thisValue, idxAlloc ? factory : new MapVariableResolverFactory(new HashMap(0), factory));
    }
    else if (switchCase != null) {
      return switchCase.getReducedValueAccelerated(ctx, thisValue, idxAlloc ? factory : new MapVariableResolverFactory(new HashMap(0), factory));
    }
    else if (defaultBlock != null) {
      return defaultBlock.getValue(ctx, thisValue, idxAlloc ? factory : new MapVariableResolverFactory(new HashMap(0), factory));
    }
    else {
      return null;
    }
  }

  public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
    if ((Boolean) eval(expr, start, offset, ctx, factory)) {
      return eval(expr, blockStart, blockOffset, ctx, new MapVariableResolverFactory(new HashMap(0), factory));
    }
    else if (switchCase != null) {
      return switchCase.getReducedValue(ctx, thisValue, new MapVariableResolverFactory(new HashMap(0), factory));
    }
    else if (defaultBlock != null) {
      return defaultBlock.getValue(ctx, thisValue, new MapVariableResolverFactory(new HashMap(0), factory));
    }
    else {
      return null;
    }
  }

  public ExecutableStatement getNestedStatement() {
    return nestedStatement;
  }

  public SwitchNode setCase(SwitchNode switchCase) {
    return this.switchCase = switchCase;
  }

  public ExecutableStatement getDefaultBlock() {
    return defaultBlock;
  }

  public SwitchNode setDefaultBlock(char[] block, int cursor, int offset, ParserContext ctx) {
    defaultBlock = (ExecutableStatement) subCompileExpression(block, cursor, offset, ctx);
    return this;
  }

  public int getBlocSwitchEnd() {
    return this.blocSwitchEnd;
  }

  public String getConditionSwitchKey() {
    return this.conditionSwitchKey;
  }

  public String toString() {
    return new String(expr, start, offset);
  }
}
