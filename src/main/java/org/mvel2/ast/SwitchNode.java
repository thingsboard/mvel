/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mvel2.ast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.mvel2.CompileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.mvel2.util.ParseTools.find;
import static org.mvel2.util.ParseTools.isWhitespace;

/**
 * @author nickAS21
 */
public class SwitchNode {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String KEY_IF = "if";
    private static final String KEY_IF_ELSE = "else if";
    private static final String KEY_ELSE = "else";
    private static final String VALUES_IF_ELSE = "values";
    private static final String EXPRESSION_IF_ELSE = "expression";
    private static final String END_BREAK = "break";
    private static final String IF_START = "if (";
    private static final String IF_ELSE_MIDDLE = "){\n";
    private static final String IF_ELSE_END = "} ";
    private static final String ELSE_IF_START = " else if (";
    private static final String ELSE_START = " else {\n";


    private char[] exprSwitchToElseIf = null;
    private int line = 0;
    private int lastLineStart = 0;
    private int cursor;

    private int start;
    private int startParameter;
    private int endParameter;
    private int endSwitchCase;
    private String parameter;
    private ObjectNode ifElse = OBJECT_MAPPER.createObjectNode();

    private char[] expr;

    public SwitchNode(char[] expr, int start, int startParameter, int endParameter, int cursor, int endSwitchCase) {
        this.expr = expr;
        this.start = start;
        this.startParameter = startParameter;
        this.endParameter = endParameter;
        this.cursor = cursor;
        this.endSwitchCase = endSwitchCase;
        parseSwitchBlock();
        setExprSwitchToElseIf();

    }

    public char[] toCharArray () {
        return exprSwitchToElseIf;
    }

    private void parseSwitchBlock() {
        this.parameter = String.copyValueOf(Arrays.copyOfRange(expr, this.startParameter, this.endParameter));
        if (parameter.length() > 0 && !parameter.trim().isEmpty())  {
            cursor++;
            skipWhitespace();
            while (cursor < this.endSwitchCase) {
                char[] expression;
                if (expr[cursor] == 'c' && expr[cursor + 1] == 'a' && expr[cursor + 2] == 's' && expr[cursor + 3] == 'e' && expr[cursor + 4] == ' ') {
                    String keyIfElse = ifElse.size() == 0 ? KEY_IF : KEY_IF_ELSE;
                    List<char[]> values = new ArrayList<>();
                    // if many "case value:" && "case value:"...
                    while (expr[cursor] == 'c' && expr[cursor + 1] == 'a' && expr[cursor + 2] == 's' && expr[cursor + 3] == 'e' && expr[cursor + 4] == ' ') {
                        cursor += 4;
                        skipWhitespace();
                        int startValue = cursor;
                        int endValue = find(expr, startValue, expr.length - startValue, ':');
                        char[] value = Arrays.copyOfRange(expr, startValue, endValue);
                        values.add(value);
                        cursor = endValue + 1;
                        skipWhitespace();
                    }
                    int startExp = cursor;
                    int endExp = 0;
                    for (int i = startExp; i < this.endSwitchCase; i++) {
                        if (expr[i] == 'b' && expr[i + 1] == 'r' && expr[i + 2] == 'e' && expr[i + 3] == 'a' && expr[i + 4] == 'k') {
                            endExp = i;
                            cursor = endExp + END_BREAK.length();
                            cursor++;
                            break;
                        }
                        if (expr[i] == 'r' && expr[i + 1] == 'e' && expr[i + 2] == 't' && expr[i + 3] == 'u' && expr[i + 4] == 'r' && expr[i + 5] == 'n' && expr[i + 6] == ' ') {
                            cursor = i + 7;
                            skipWhitespace();
                            while (!(expr[cursor] == 'c' && expr[cursor + 1] == 'a' && expr[cursor + 2] == 's' && expr[cursor + 3] == 'e' && expr[cursor + 4] == ' ') &&
                                    !(expr[cursor] == 'd' && expr[cursor + 1] == 'e' && expr[cursor + 2] == 'f' && expr[cursor + 3] == 'a' && expr[cursor + 4] == 'u'
                                            && expr[cursor + 5] == 'l' && expr[cursor + 6] == 't') && cursor < this.endSwitchCase) {
                                endExp = cursor;
                                cursor++;
                            }
                            break;
                        }

                    }
                    if (endExp == 0) {
                        cursor = this.endSwitchCase;
                        endExp = this.endSwitchCase - 1;
                    }
                    expression = Arrays.copyOfRange(expr, startExp, endExp);
                    addIfElse(keyIfElse, values, expression);
                    skipWhitespace();
                    if (expr[cursor] == ';') {
                        cursor++;
                        skipWhitespace();
                    }
                } else if (expr[cursor] == 'd' && expr[cursor + 1] == 'e' && expr[cursor + 2] == 'f' && expr[cursor + 3] == 'a' && expr[cursor + 4] == 'u'
                        && expr[cursor + 5] == 'l' && expr[cursor + 6] == 't') {
                    if (ifElse.size() != 0) {
                        cursor += 7;
                        skipWhitespace();
                        if (expr[cursor] == ':') {
                            cursor++;
                            skipWhitespace();
                            expression = Arrays.copyOfRange(expr, cursor, this.endSwitchCase);
                            addIfElse(KEY_ELSE, null, expression);
                            cursor = this.endSwitchCase + 1;
                        } else {
                            throw new CompileException("Switch default Unrecoverable syntax error.", expr, start);
                        }
                    } else {
                        throw new CompileException("Switch default without case.", expr, start);
                    }
                } else {
                    throw new CompileException("Switch without case and default.", expr, start);
                }
            }

        } else {
            throw new CompileException("Switch without expression.", expr, start);
        }
    }

    private void setExprSwitchToElseIf () {
        if (this.ifElse.size() > 0) {
            String condition = "";
            for (Object val : this.ifElse.get(KEY_IF).get(VALUES_IF_ELSE)) {
                if (condition.isEmpty()) {
                    condition = this.parameter + " === " + ((TextNode) val).textValue();
                } else {
                    condition += " || " + this.parameter + " === " + ((TextNode) val).textValue();
                }
            }
            String expression = this.ifElse.get(KEY_IF).get(EXPRESSION_IF_ELSE).asText().trim();
            String switchToElseIf = IF_START + condition + IF_ELSE_MIDDLE + "    " + expression + "\n" + IF_ELSE_END;

            if (this.ifElse.has(KEY_IF_ELSE)) {
                for (JsonNode node: this.ifElse.get(KEY_IF_ELSE)) {
                    condition = "";
                    for (Object val : node.get(VALUES_IF_ELSE)) {
                        if (condition.isEmpty()) {
                            condition = this.parameter + " === " + ((TextNode) val).textValue();
                        } else {
                            condition += " || " + this.parameter + " === " + ((TextNode) val).textValue();
                        }
                    }
                    expression = node.get(EXPRESSION_IF_ELSE).asText().trim();
                    switchToElseIf += (ELSE_IF_START + condition + IF_ELSE_MIDDLE + "    " + expression + "\n"  + IF_ELSE_END);
                }
            }

            if (this.ifElse.has(KEY_ELSE)) {
                expression = this.ifElse.get(KEY_ELSE).get(EXPRESSION_IF_ELSE).asText().trim();
                switchToElseIf += (ELSE_START + "    " + expression + "\n" + IF_ELSE_END);
            }
            switchToElseIf += "\n";
            this.exprSwitchToElseIf = switchToElseIf.toCharArray();
        }
    }

    private void addIfElse (String keyIfElse, List<char[]> values, char[] expression) {
        try {
            ArrayNode arrayValues = values != null ? OBJECT_MAPPER.valueToTree(values) : null;
            TextNode textExpression = OBJECT_MAPPER.valueToTree(expression);
            switch (keyIfElse) {
                case KEY_IF:
                    ObjectNode addedNodeIf = this.ifElse.putObject(KEY_IF);
                    arrayValues = OBJECT_MAPPER.valueToTree(values);
                    addedNodeIf.putIfAbsent(VALUES_IF_ELSE, arrayValues);
                    addedNodeIf.putIfAbsent(EXPRESSION_IF_ELSE, textExpression);
                    break;
                case KEY_IF_ELSE:
                    arrayValues = OBJECT_MAPPER.valueToTree(values);
                    ObjectNode addedNodeIfElse = OBJECT_MAPPER.createObjectNode();
                    addedNodeIfElse.putIfAbsent(VALUES_IF_ELSE, arrayValues);
                    addedNodeIfElse.putIfAbsent(EXPRESSION_IF_ELSE, textExpression);
                    if (this.ifElse.has(KEY_IF_ELSE) ) {
                        ((ArrayNode)this.ifElse.get(KEY_IF_ELSE)).add(addedNodeIfElse);
                    } else {
                        ArrayNode arrayNodeIfElseS = OBJECT_MAPPER.createArrayNode();
                        arrayNodeIfElseS.add(addedNodeIfElse);
                        this.ifElse.putIfAbsent(KEY_IF_ELSE, arrayNodeIfElseS);
                    }
                    break;
                case KEY_ELSE:
                    ObjectNode addedNodeElse = this.ifElse.putObject(KEY_ELSE);
                    addedNodeElse.putIfAbsent(EXPRESSION_IF_ELSE, textExpression);
            }
        } catch (Exception e) {
            throw new CompileException("Switch parse json failed. " + e.getMessage(), expr, start);
        }
    }

    private void skipWhitespace() {
        Skip:
        while (cursor != this.endSwitchCase) {
            switch (expr[cursor]) {
                case '\n':
                    line++;
                    lastLineStart = cursor;
                case '\r':
                    cursor++;
                    continue;
                case '/':
                    if (cursor + 1 != this.endSwitchCase) {
                        switch (expr[cursor + 1]) {
                            case '/':

                                expr[cursor++] = ' ';
                                while (cursor != this.endSwitchCase && expr[cursor] != '\n') {
                                    expr[cursor++] = ' ';
                                }
                                if (cursor != this.endSwitchCase) {
                                    cursor++;
                                }

                                line++;
                                lastLineStart = cursor;

                                continue;

                            case '*':
                                int len = this.endSwitchCase - 1;
                                int st = cursor;
                                cursor++;

                                while (cursor != len && !(expr[cursor] == '*' && expr[cursor + 1] == '/')) {
                                    cursor++;
                                }
                                if (cursor != len) {
                                    cursor += 2;
                                }

                                for (int i = st; i < cursor; i++) {
                                    expr[i] = ' ';
                                }

                                continue;

                            default:
                                break Skip;

                        }
                    }
                default:
                    if (!isWhitespace(expr[cursor])) break Skip;

            }
            cursor++;
        }
    }
}


