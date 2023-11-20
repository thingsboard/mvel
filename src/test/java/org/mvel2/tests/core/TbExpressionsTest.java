package org.mvel2.tests.core;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.mvel2.CompileException;
import org.mvel2.ExecutionContext;
import org.mvel2.ParserContext;
import org.mvel2.SandboxedParserConfiguration;
import org.mvel2.ScriptMemoryOverflowException;
import org.mvel2.ScriptRuntimeException;
import org.mvel2.execution.ExecutionArrayList;
import org.mvel2.execution.ExecutionHashMap;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.util.MethodStub;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeTbExpression;

public class TbExpressionsTest extends TestCase {

    private static final Comparator numericCompAsc = new Comparator() {
        public int compare(Object o1, Object o2) {
            Double first = Double.parseDouble(String.valueOf(o1));
            Double second = Double.parseDouble(String.valueOf(o2));
            return first.compareTo(second);
        }
    };

    private SandboxedParserConfiguration parserConfig;

    private ExecutionContext currentExecutionContext;

    @Override
    protected void setUp() throws Exception {
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
        super.setUp();
        this.parserConfig = ParserContext.enableSandboxedMode();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        ParserContext.disableSandboxedMode();
    }

    public void testCreateSingleValueArray() {
        Object res = executeScript("m = {5}; m");
        assertTrue(res instanceof List);
        assertEquals(1, ((List) res).size());
        assertEquals(5, ((List) res).get(0));
    }

    public void testCreateMap() {
        Object res = executeScript("m = {a: 1}; m.a");
        assertTrue(res instanceof Integer);
        assertEquals(1, res);
    }

    public void testCreateEmptyMapAndAssignField() {
        Object res = executeScript("m = {}; m.test = 1; m");
        assertTrue(res instanceof Map);
        assertEquals(1, ((Map) res).size());
        assertEquals(1, ((Map) res).get("test"));
    }

    public void testAssignmentWhitespaces() {
        Object res = executeScript("var m= 2; m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("var m=2; m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("var m =2; m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("m=2; m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("m= 2; m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("int m= 2; m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("int m=2; m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("int m=2; m=3; m= 4; m");
        assertTrue(res instanceof Integer);
        assertEquals(4, res);
        res = executeScript("int m=2; m+=3; m");
        assertTrue(res instanceof Integer);
        assertEquals(5, res);
        res = executeScript("int m=2; m +=3; m");
        assertTrue(res instanceof Integer);
        assertEquals(5, res);
        res = executeScript("int m=2; m+= 3; m");
        assertTrue(res instanceof Integer);
        assertEquals(5, res);
        res = executeScript("int m=2; m\n+=\n3; m");
        assertTrue(res instanceof Integer);
        assertEquals(5, res);
        res = executeScript("var \n\n  m= 2;     m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("var   m= 2;     m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("var  m= 2;     m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("var m= 2;     m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("int   m =  2;     m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("  \n\n    m  \n\n  =  \n\n 2;  \n\n   m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("  \n\n  var \n\n  m  \n\n  =  \n\n 2;  \n\n   m");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
    }

    public void testNonExistentMapField() {
        Object res = executeScript("m = {}; t = m.test; t");
        assertNull(res);
    }

    public void testEqualsOperator() {
        Object res = executeScript("m = 'abc'; m === 'abc'");
        assertTrue(res instanceof Boolean);
        assertTrue((Boolean) res);
        res = executeScript("m = 'abc'; m = 1; m == 1");
        assertTrue(res instanceof Boolean);
        assertTrue((Boolean) res);
    }

    public void testFunctionOrder() {
        Object res = executeScript("function testFunc(m) {m.a +=1;} m = {a: 1}; testFunc(m);   m.a");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
        res = executeScript("m = {a: 1}; testFunc(m); function testFunc(m) {m.a +=1;}  m.a");
        assertTrue(res instanceof Integer);
        assertEquals(2, res);
    }

    public void testVariableScope() {
       Object res = executeScript("var m = 25; " +
                                   "function testFunc(a) {" +
                                   "   function testFunc3(e) {" +
                                   "       var m;" +
                                   "       m = e + 5\n; " +
                                   "       return m" +
                                   "   };" +
                                   "   var t = 2;\n" +
                                   "   m = a * t;" +
                                   "   return testFunc3(testFunc2(m + t));" +
                                   "}" +
                                   "function testFunc2(b) {" +
                                   "   var c = 3;m = b * c; return m;" +
                                   "}" +
                                   "function testFunc4(m) {" +
                                   "   return m * 2;" +
                                   "}" +
                                   "var m2 = m + testFunc(m); \n" +
                                   "return testFunc4(m2)");
        assertTrue(res instanceof Integer);
        assertEquals((25 + ((25 * 2 + 2) * 3) + 5) * 2, res);

        res = executeScript("var array = [1, 2, 3];\n" +
                "function sum(array){\n" +
                "    var result = 0;\n" +
                "    for(int i = 0; i < array.length; i++){\n" +
                "        result += array[i];\n" +
                "        var element = array[i];\n" +
                "        result += element;\n" +
                "    }\n" +
                "    return result;\n" +
                "}\n" +
                "return sum(array)");
        assertTrue(res instanceof Integer);
        assertEquals(12, res);

        res = executeScript("var array = [1, 2, 3];\n" +
                "    var result = 0;\n" +
                "    for(int i = 0; i < array.length; i++){\n" +
                "        result += array[i];\n" +
                "        var element = array[i];\n" +
                "        result += element;\n" +
                "    }\n" +
                "return result");
        assertTrue(res instanceof Integer);
        assertEquals(12, res);

        res = executeScript("var array = [1, 2, 3];\n" +
                "function sum(array){\n" +
                "    var result = 0;\n" +
                "    var i = 0;\n" +
                "    while(i < array.length){\n" +
                "        result += array[i];\n" +
                "        var element = array[i];\n" +
                "        result += element;\n" +
                "        i++;\n" +
                "    }\n" +
                "    return result;\n" +
                "}\n" +
                "return sum(array)");
        assertTrue(res instanceof Integer);
        assertEquals(12, res);

        res = executeScript("var array = [1, 2, 3];\n" +
                "    var result = 0;\n" +
                "    var i = 0;\n" +
                "    while(i < array.length){\n" +
                "        result += array[i];\n" +
                "        var element = array[i];\n" +
                "        result += element;\n" +
                "        i++;\n" +
                "    }\n" +
                "return result");
        assertTrue(res instanceof Integer);
        assertEquals(12, res);
    }

    public void testComments() {
        Object res = executeScript("//var df = sdfsdf; \n // test comment: comment2 \n m = {\n// c: d, \n /* e: \n\nf, */ a: 2 }; m");
        assertTrue(res instanceof HashMap);
        assertEquals(1, ((Map) res).size());
        assertEquals(2, ((Map) res).get("a"));
    }

    public void testStopExecution() throws Exception {
        AtomicReference<Exception> capturedException = new AtomicReference<>();
        final CountDownLatch countDown = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            try {
                executeScript("t = 0; while(true) { t  = 1}; t");
            } catch (Exception e) {
                capturedException.set(e);
            } finally {
                countDown.countDown();
            }
        });
        thread.start();
        boolean result = countDown.await(500, TimeUnit.MILLISECONDS);
        assertFalse(result);
        this.currentExecutionContext.stop();
        result = countDown.await(500, TimeUnit.MILLISECONDS);
        assertTrue(result);
        Exception exception = capturedException.get();
        assertNotNull(exception);
        assertEquals("Script execution is stopped!", exception.getMessage());
    }

    public void testMemoryOverflowVariable() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("t = 'abc'; while(true) { t  += t}; t", new HashMap(), new ExecutionContext(parserConfig, memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
    }

    public void testMemoryOverflowInnerVariable() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("doMemoryOverflow(); function doMemoryOverflow() { var t = 'abc'; while(true) { t  += t}; } ", new HashMap(), new ExecutionContext(parserConfig, memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
    }

    public void testMemoryOverflowInnerMap1() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("m = {}; m.put('a', {}); i =0; while(true) { m.get('a').put(i++, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' + Math.random());}; m", new HashMap(), new ExecutionContext(parserConfig, memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
    }

    public void testMemoryOverflowInnerMap2() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("m = {}; m.a = {}; i =0; while(true) { m.a[i++] = 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' + Math.random();}; m", new HashMap(), new ExecutionContext(parserConfig, memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
    }

    public void testMemoryOverflowArray() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("m = []; i =0; while(true) { m.add('aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' + Math.random())}; m", new HashMap(), new ExecutionContext(parserConfig, memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
    }

    public void testMemoryOverflowArrayInnerMap() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("m = [1]; m[0] = {}; i =0; while(true) { m[0].put(i++, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' + Math.random())}; m", new HashMap(), new ExecutionContext(parserConfig, memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
    }

    public void testMemoryOverflowAddAll() throws Exception {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("a = ['aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa']; b = []; while(true) { b.addAll(a)}; m", new HashMap(), new ExecutionContext(parserConfig, memoryLimit), 10000);
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit));
        }
    }

    public void testArrayMemoryOverflow() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("m = new byte[5 * 1024 * 1024]; m", new HashMap(), new ExecutionContext(parserConfig, memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Max array length overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit / 2));
        }
    }

    public void testMethodArgumentsLength() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        int argsLimit = 5;
        try {
            executeScript("var s = '%s'; for (var i = 0; i < 20; i++) { s = s + s; }\n\n" +
                              "return '\\n12Result is :\\n' + String.format(s, s, s, s, s, s, s, " +
                    "s, s, s, s);", new HashMap(), new ExecutionContext(parserConfig, memoryLimit, argsLimit));
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("Maximum method arguments count overflow"));
            assertTrue(e.getMessage().contains("" + argsLimit));
        }
    }

    public void testMethodInvocationForStringRepeat() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("'a'.repeat(Integer.MAX_VALUE-100)", new HashMap(), new ExecutionContext(parserConfig, memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Max string length overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit / 2));
        }
    }

    public void testMethodInvocationForStringConcat() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("var toConcat = 'a'.repeat(5 * 1024 * 1024 / 2 - 'abc'.length() + 1); 'abc'.concat(toConcat);", new HashMap(), new ExecutionContext(parserConfig, memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Max string length overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit / 2));
        }
    }

    public void testMethodInvocationForStringReplace() {
        long memoryLimit = 5 * 1024 * 1024; // 5MB
        try {
            executeScript("var repl = 'a'.repeat(5 * 1024 * 1024 / 100 + 1); 'abc'.replace('a', repl);", new HashMap(), new ExecutionContext(parserConfig, memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Max replacement length overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit / 100));
        }
        try {
            executeScript("var repl = 'a'.repeat(5 * 1024 * 1024 / 100 + 1); 'abc'.replaceAll('a', repl);", new HashMap(), new ExecutionContext(parserConfig, memoryLimit));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Max replacement length overflow"));
            assertTrue(e.getMessage().contains("" + memoryLimit / 100));
        }
    }

    public void testForbidCustomObjects() {
        try {
            executeScript("m = new java.util.HashMap(); m");
            fail("Should throw ScriptRuntimeException");
        } catch (ScriptRuntimeException e) {
            assertTrue(e.getMessage().contains("Invalid statement: new java.util.HashMap()"));
        }
    }

    public void testForbiddenMethodAccess() {
        try {
            this.parserConfig.addImport("JSON", MyTestClass.class);
            executeScript("JSON.getModule().getClassLoader().loadClass(\"java.lang.Runtime\")");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unable to resolve method: " + MyTestClass.class.getName() + ".getModule"));
        }
    }

    public void testForbiddenClassAccess() {
        try {
            executeScript("new StringBuffer();");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("could not resolve class: StringBuffer"));
        }

        try {
            executeScript("new java.lang.StringBuffer();");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("could not resolve class: java.lang.StringBuffer"));
        }

        try {
            executeScript("new StringBuilder();");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("could not resolve class: StringBuilder"));
        }

        try {
            executeScript("new java.lang.StringBuilder();");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("could not resolve class: java.lang.StringBuilder"));
        }

        try {
            executeScript("\n\nClass c;");
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unknown class or illegal statement: Class"));
        }
        try {
            executeScript("m = {5}; System.exit(-1); m");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: System"));
        }

        try {
            executeScript("m = {5}; for (int i=0;i<10;i++) {System.exit(-1);}; m");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: System"));
        }

        try {
            executeScript("m = {5}; function test() {System.exit(-1);}; test(); m");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: System"));
        }

        try {
            executeScript("m = {5}; exit(-1); m");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("function not found: exit"));
        }

        try {
            executeScript("m = {5}; java.lang.System.exit(-1); m");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: java"));
        }

        try {
            executeScript("m = {5}; Runtime.getRuntime().exec(\"echo hi\"); m");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: Runtime"));
        }

        try {
            executeScript("m = {5}; m.getClass().getClassLoader()");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unable to resolve method: getClass()"));
        }

        try {
            executeScript("Array.newInstance(Object, 1000, 1000, 1000, 1000);");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: Array"));
        }

        try {
            executeScript("m = {5}; m.class");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("could not access property: class"));
        }

        try {
            executeScript("m = new java.io.File(\"password.txt\").exists(); m");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("could not resolve class: java.io.File"));
        }

        try {
            executeScript("m = MVEL.eval(\"System.exit(-1);\"); m");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: MVEL"));
        }
        try {
            executeScript("m = org.mvel2.MVEL.eval(\"System.exit(-1);\"); m");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: org"));
        }
        try {
            executeScript("java.util.concurrent.Executors.newFixedThreadPool(2)");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: java"));
        }
        Object res = executeScript("m = {class: 5}; m.class");
        assertNotNull(res);
        assertEquals(5, res);
    }

    public void testForbidImport() {
        try {
            executeScript("import java.util.HashMap; m = new HashMap(); m.put('t', 10); m");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("Import is forbidden!"));
        }
    }

    public void testPrimitiveArray() {
        Object res = executeScript("var m = new byte[1000]; m[5] = 1; m[500] = 20; m[40] = 0x0B; m");
        assertNotNull(res);
        assertTrue(res instanceof List);
        assertEquals(1000, ((List<?>) res).size());
        assertEquals((byte)1, ((List<?>) res).get(5));
        assertEquals((byte)20, ((List<?>) res).get(500));
        assertEquals((byte)0x0B, ((List<?>) res).get(40));
        res = executeScript("var m = 'Hello world'; a = m.toCharArray(); a");
        assertNotNull(res);
        assertTrue(res instanceof List);
        Object[] boxedArray = ((List)res).toArray();
        int len = boxedArray.length;
        char[] array = new char[len];
        for (int i = 0; i < len; i++) {
            array[i] = (Character) boxedArray[i];
        }
        assertEquals("Hello world", String.valueOf(array));
        res = executeScript("var m = new int[]{1, 2, 3}; m");
        assertNotNull(res);
        assertTrue(res instanceof List);
        assertEquals(3, ((List<?>) res).size());
        assertEquals(1, ((List<?>) res).get(0));
        assertEquals(2, ((List<?>) res).get(1));
        assertEquals(3, ((List<?>) res).get(2));
    }

    public void testByteOperations() {
        Object res = executeScript("var m = new byte[]{(byte)0x0F,(byte)0x02}; b = m[0] == 0x0F; b");
        assertNotNull(res);
        assertTrue(res instanceof Boolean);
        assertTrue((Boolean) res);
        res = executeScript("var a = 0x0F; b = 0x0A; c = a + b; c");
        assertNotNull(res);
        assertTrue(res instanceof Integer);
        assertEquals(25, res);
        res = executeScript("var a = (byte)0x0F; b = a << 24 >> 16; b");
        assertNotNull(res);
        assertEquals((byte)0x0F << 24 >> 16, res);
    }

    public void testUnterminatedStatement() {
        Object res = executeScript("var a = \"A\";\n" +
                "var b = \"B\"\n;" +
                "var c = \"C\"\n;" +
                "result = a + b\n;" +
                "result = c\t\n;\n" +
                "{msg: result, \n\nmetadata: {}, msgType: ''}");
        assertNotNull(res);
        assertTrue(res instanceof Map);
        assertEquals("C", ((Map<?, ?>) res).get("msg"));

        try {
            executeScript("var a = \"A\";\n" +
                    "var b = \"B\"\n;" +
                    "var c = \"C\"\n;" +
                    "result = a + b\n;" +
                    "result = c\n\n" +
                    "{msg: result, \n\nmetadata: {}, msgType: ''}");
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().equals("[Error: Unterminated statement!]\n" +
                    "[Near : {... ;result = c ....}]\n" +
                    "                       ^\n" +
                    "[Line: 5, Column: 11]"));
        }
    }

    public void testStringTermination() {
        Object res = executeScript("var c = 'abc' + \n" +
                "'def1' + ('a' + 'b') + \n " +
                "'def2';\n c");
        assertNotNull(res);
        assertEquals("abcdef1abdef2", res);
    }

    public void testRuntimeAndCompileErrors() {
        try {
            executeScript("threshold = def (x) { x >= 10 ? x : 0 };\n" +
                    "result = cost + threshold(lowerBound);");
            fail("Should throw ScriptRuntimeException");
        } catch (ScriptRuntimeException e) {
            assertTrue(e.getMessage().equals("[Error: Invalid statement: def (x) { x >= 10 ? x : 0 }]\n" +
                    "[Near : {... threshold = def (x) { x >= 10 ? x : 0 }; ....}]\n" +
                    "                         ^\n" +
                    "[Line: 1, Column: 13]"));
        }
        try {
            executeScript("a = [1,2;\n");
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().equals("[Error: unbalanced braces [ ... ]]\n" +
                    "[Near : {... a = [1,2; ....}]\n" +
                    "                 ^\n" +
                    "[Line: 1, Column: 5]"));
        }
        try {
            executeScript("a = [1,2];\n function c(a) {\nvar b = 0;\nb += a[0].toInt();\nreturn b;}\n c(a);");
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().equals("[Error: unable to resolve method: java.lang.Integer.toInt() [arglength=0]]\n" +
                    "[Near : {... b += a[0].toInt(); ....}]\n" +
                    "                 ^\n" +
                    "[Line: 4, Column: 5]"));
        }
    }

    public void testUseClassImport() {
        try {
            executeScript("MyTestUtil.getFoo({foo: 'foo-bar'})");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            Assert.assertTrue(e.getMessage().contains("unresolvable property or identifier: MyTestUtil"));
        }
        this.parserConfig.addImport("MyTestUtil", TestUtil.class);
        Object res = executeScript("MyTestUtil.getFoo({foo: 'foo-bar'})");
        assertEquals("foo-bar", res);
        res = executeScript("MyTestUtil.getFoo({})");
        assertEquals("Not found!", res);
        res = executeScript("MyTestUtil.methodWithExecContext('key1', 'val1')");
        assertTrue(res instanceof Map);
        assertEquals("val1", ((Map)res).get("key1"));
        res = executeScript("MyTestUtil.methodWithExecContext2('key2', 'val2')");
        assertTrue(res instanceof Map);
        assertEquals("val2", ((Map)res).get("key2"));
        res = executeScript("MyTestUtil.methodWithExecContext3('key3', 'val3')");
        assertTrue(res instanceof Map);
        assertEquals("val3", ((Map)res).get("key3"));
        res = executeScript("MyTestUtil.methodWithExecContextVarArgs('a1', 'a2', 'a3', 'a4', 'a5')");
        assertTrue(res instanceof List);
        assertEquals(5, ((List)res).size());
        assertArrayEquals(new String[]{"a1", "a2", "a3", "a4", "a5"}, ((List)res).toArray(new String[5]));
    }

    public void testUseStaticMethodImport() throws Exception {
        this.parserConfig.addImport("getFoo", new MethodStub(TestUtil.class.getMethod("getFoo",
                Map.class)));
        Object res = executeScript("getFoo({foo: 'foo-bar'})");
        assertEquals("foo-bar", res);
        res = executeScript("getFoo({})");
        assertEquals("Not found!", res);
        try {
            executeScript("currentTimeMillis()");
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            Assert.assertTrue(e.getMessage().contains("function not found: currentTimeMillis"));
        }
        this.parserConfig.addImport("currentTimeMillis", new MethodStub(System.class.getMethod("currentTimeMillis")));
        res = executeScript("currentTimeMillis()");
        assertTrue(res instanceof Long);
        assertEquals(System.currentTimeMillis() / 100, ((long) res) / 100);
        this.parserConfig.addImport("methodWithExecContext", new MethodStub(TestUtil.class.getMethod("methodWithExecContext",
                String.class, Object.class, ExecutionContext.class)));
        res = executeScript("methodWithExecContext('key1', 'val1')");
        assertTrue(res instanceof Map);
        assertEquals("val1", ((Map)res).get("key1"));
        this.parserConfig.addImport("methodWithExecContext2", new MethodStub(TestUtil.class.getMethod("methodWithExecContext2",
                String.class, ExecutionContext.class, Object.class)));
        res = executeScript("methodWithExecContext2('key2', 'val2')");
        assertTrue(res instanceof Map);
        assertEquals("val2", ((Map)res).get("key2"));
        this.parserConfig.addImport("methodWithExecContext3", new MethodStub(TestUtil.class.getMethod("methodWithExecContext3",
                ExecutionContext.class, String.class, Object.class)));
        res = executeScript("methodWithExecContext3('key3', 'val3')");
        assertTrue(res instanceof Map);
        assertEquals("val3", ((Map)res).get("key3"));
        this.parserConfig.addImport("methodWithExecContextVarArgs", new MethodStub(TestUtil.class.getMethod("methodWithExecContextVarArgs",
                ExecutionContext.class, Object[].class)));
        res = executeScript("methodWithExecContextVarArgs('a1', 'a2', 'a3', 'a4', 'a5')");
        assertTrue(res instanceof List);
        assertEquals(5, ((List)res).size());
        assertArrayEquals(new String[]{"a1", "a2", "a3", "a4", "a5"}, ((List)res).toArray(new String[5]));
    }

    public void testRegisterDataType() {
        try {
            executeScript("var t = new MyTest('test val'); t");
            fail("Should throw CompileException");
        } catch (CompileException e) {
            Assert.assertTrue(e.getMessage().contains("could not resolve class: MyTest"));
        }
        this.parserConfig.registerDataType("MyTest", MyTestClass.class, val -> (long)val.getValue().getBytes().length);
        Object res = executeScript("var t = new MyTest('test val'); t");
        assertTrue(res instanceof MyTestClass);
        assertEquals("test val", ((MyTestClass)res).getValue());
        try {
            executeScript("var t = new MyTest('test val'); t", new HashMap(), new ExecutionContext(parserConfig, 7));
            fail("Should throw ScriptMemoryOverflowException");
        } catch (ScriptMemoryOverflowException e) {
            assertTrue(e.getMessage().contains("Script memory overflow"));
            assertTrue(e.getMessage().contains("8 > 7"));
        }
    }

    public void testDate() {
        Object res = executeScript("var t = new java.util.Date(); t");
        assertTrue(res instanceof Date);
        assertEquals(System.currentTimeMillis() / 100, ((Date) res).getTime() / 100);
        res = executeScript("var t = new java.util.Date(); var m = {date: t}; m");
        assertTrue(res instanceof Map);
        assertTrue(((Map<?, ?>) res).get("date") instanceof Date);
    }

    public void testIdIdJsFormat() {
        String scriptBodyTestIdIdPointStr = "var msg = {};\n" +
                "var msg_sub = {};\n" +
                "msg_sub[\"entityGroup\"] = \"ENTITY_GROUP\";\n" +
                "msg_sub[\"id2\"] = \"4e7b7e10-c27d-11ed-a0ab-058763ffd58e\";\n" +
                "msg[\"id1\"] =  msg_sub;\n" +
                "return {\n" +
                "    msg: {\n" +
                "        entryPoint_bad: \"/api/entityGroup/\" + msg.id1.id2\n" +
                "    }\n" +
                "};";

        String scriptBodyTestIdIdJsFormatStr = "var msg = {};\n" +
                "var msg_sub = {};\n" +
                "msg_sub[\"entityGroup\"] = \"ENTITY_GROUP\";\n" +
                "msg_sub[\"id2\"] = \"4e7b7e10-c27d-11ed-a0ab-058763ffd58e\";\n" +
                "msg[\"id1\"] =  msg_sub;\n" +
                "return {\n" +
                "    msg: {\n" +
                "        entryPoint_bad: \"/api/entityGroup/\" + msg[\"id1\"][\"id2\"]\n" +
                "    }\n" +
                "};";
        Object actual = executeScript(scriptBodyTestIdIdJsFormatStr);
        Object expected = executeScript(scriptBodyTestIdIdPointStr);

        assertEquals(expected, actual);
    }

    public void testSwitchNodeStandardCaseOneValue_Ok() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 120;\n" +
                "switch (msg.temperature){\n" +
                "    case 120.0:\n" +
                "        msg.temperature = 1.0;\n" +
                "         break;\n" +
                "    case 241:\n" +
                "        msg.temperature = 2.0;\n" +
                "        break;\n" +
                "    default:\n" +
                "        msg.temperature = 3.0;\n" +
                "\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        Double dd = 1.0;
        expected.put("temp", dd);
        Object actual = executeScript(scriptBodyTestSwitchNodeStr);
        assertEquals(expected, actual);
    }
    public void testSwitchNodeStandardCaseTwoValue_Ok() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 241;\n" +
                "switch (msg.temperature){\n" +
                "    case 120.0:\n" +
                "        msg.temperature = 1.0;\n" +
                "         break;\n" +
                "    case 241:\n" +
                "        msg.temperature = 2.0;\n" +
                "        break;\n" +
                "    default:\n" +
                "        msg.temperature = 3.0;\n" +
                "\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        Double dd = 2.0;
        expected.put("temp", dd);
        Object actual = executeScript(scriptBodyTestSwitchNodeStr);
        assertEquals(expected, actual);
    }
    public void testSwitchNodeStandardDefault_Ok() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 130.0;\n" +
                "switch (msg.temperature){\n" +
                "    case 120.0:\n" +
                "        msg.temperature = 1.0;\n" +
                "         break;\n" +
                "    case 241:\n" +
                "        msg.temperature = 2.0;\n" +
                "        break;\n" +
                "    default:\n" +
                "        msg.temperature = msg.temperature;\n" +
                "\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        Double dd = 130.0;
        expected.put("temp", dd);
        Object actual = executeScript(scriptBodyTestSwitchNodeStr);
        assertEquals(expected, actual);
    }
    public void testSwitchNodeOnlyCase_Ok() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 120.0;\n" +
                "switch (msg.temperature){\n" +
                "    case 120.0:\n" +
                "        msg.temperature = 1.0;\n" +
                "\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        Double dd = 1.0;
        expected.put("temp", dd);
        Object actual = executeScript(scriptBodyTestSwitchNodeStr);
        assertEquals(expected, actual);
    }

    public void testSwitchNodeCaseDoubleValue_Ok() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 123;\n" +
                "switch (msg.temperature){\n" +
                "    case 120.0:\n" +
                "    case 123.0:\n" +
                "    case 126.0:\n" +
                "        msg.temperature = 1.0;\n" +
                "         break;\n" +
                "    case 241:\n" +
                "        msg.temperature = 2.0;\n" +
                "        break;\n" +
                "    default:\n" +
                "        msg.temperature = 3.0;\n" +
                "\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        Double dd = 1.0;
        expected.put("temp", dd);
        Object actual = executeScript(scriptBodyTestSwitchNodeStr);
        assertEquals(expected, actual);
    }

    public void testSwitchNodeSwitchInSwitch_Ok() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 19.0;\n" +
                "var a = 25;\n" +
                "\n" +
                "if (msg.temperature === 19){\n" +
                "    msg.temperature = 20.0;\n" +
                "} else if (msg.temperature === 24){\n" +
                "    msg.temperature = 23.11;\n" +
                "}  else {\n" +
                "    msg.temperature = msg.temperature;\n" +
                "}\n" +
                "\n" +
                "switch (msg.temperature){\n" +
                "    case 20.0:\n" +
                "    case 120.0:\n" +
                "         switch (msg.temperature){\n" +
                "            case 122.4:\n" +
                "            case 100.0:\n" +
                "                 msg.temperature = 130.1;\n" +
                "                 break;\n" +
                "            case 112.0:\n" +
                "                msg.temperature = 101;\n" +
                "                break;\n" +
                "            case 20.0:\n" +
                "                msg.temperature = 105.6789;\n" +
                "                msg.temperature += a;\n" +
                "                msg.temperature = msg.temperature/2;\n" +
                "                break;\n" +
                "           default:\n" +
                "                msg.temperature = 105.6789;\n" +
                "                msg.temperature += a;\n" +
                "                msg.temperature = msg.temperature/2;\n" +
                "        }\n" +
                "         break;\n" +
                "    case 12.0:\n" +
                "        msg.temperature = 1;\n" +
                "        break;\n" +
                "    case 15.0:\n" +
                "        msg.temperature = 5;\n" +
                "        break;\n" +
                "    default:\n" +
                "        msg.temperature = 2;\n" +
                "\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        Double dd = (105.6789 + 25)/2;
        expected.put("temp", dd);
        Object actual = executeScript(scriptBodyTestSwitchNodeStr);
        assertEquals(expected, actual);
    }

    public void testSwitchNodeParameterString_Ok() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 120.0;\n" +
                "var par = \"test1\";\n" +
                "\n" +
                "switch (par){\n" +
                "    case \"test\":\n" +
                "         msg.temperature = 1.0;\n" +
                "         break;\n" +
                "    case \"test1\":\n" +
                "        msg.temperature = 2.0;\n" +
                "        break;\n" +
                "    case 15.0:\n" +
                "        msg.temperature = 3;\n" +
                "        break;\n" +
                "    default:\n" +
                "        msg.temperature = 4;\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        Double dd = 2.0;
        expected.put("temp", dd);
        Object actual = executeScript(scriptBodyTestSwitchNodeStr);
        assertEquals(expected, actual);
    }

    public void testSwitchNodeInFunctionWithReturn_Ok() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 120.0;\n" +
                "var par = \"test\";\n" +
                "\n" +
                "switch (par){\n" +
                "    case \"test\":\n" +
                "         msg.temperature = switchReturn(par) ;\n" +
                "         return {temp: msg.temperature};\n" +
                "    case \"test2\":\n" +
                "         msg.temperature = 1.0;\n" +
                "}\n" +
                "return {temp: msg.temperature};\n" +
                "function switchReturn(val) {\n" +
                "    switch (val) {\n" +
                "        case \"test\":\n" +
                "            return 48.0;\n" +
                "        case 12.0:\n" +
                "            msg.temperature = 4.0 / 2;\n" +
                "           return 5.0;\n" +
                "        case 15.0:\n" +
                "            msg.temperature = 3;\n" +
                "            break;\n" +
                "        default:\n" +
                "            msg.temperature = msg.temperature;\n" +
                "    }\n" +
                "    return 48;\n" +
                "\n" +
                "}";

        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        Double dd = 48.0;
        expected.put("temp", dd);
        Object actual = executeScript(scriptBodyTestSwitchNodeStr);
        assertEquals(expected, actual);
    }

    public void testSwitchNodeWithReturnInCase_Ok() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 120.0;\n" +
                "var par = \"test\";\n" +
                "\n" +
                "switch (par){\n" +
                "    case \"test\":\n" +
                "         msg.temperature = 3.0;\n" +
                "         return {temp: msg.temperature};\n" +
                "    case \"test2\":\n" +
                "         msg.temperature = 1.0;\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        Double dd = 3.0;
        expected.put("temp", dd);
        Object actual = executeScript(scriptBodyTestSwitchNodeStr);
        assertEquals(expected, actual);
    }

    public void testSwitchNodeWithComments_Ok() {
        String scriptBodyTestSwitchNodeStr = " \n" +
                "//switch (parCase){\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 120.0;\n" +
                "var parCase = 15.0;\n" +
                "\n" +
                "switch (parCase){\n" +
                "    case \"test\":\n" +
                "        /**\n" +
                "         // commit3\n" +
                "         **/\n" +
                "         msg.temperature = 1.0;\n" +
                "        // bdreak_stop; \n" +
                "         break;\n" +
                "    case 12.0:\n" +
                "        /*\n" +
                "        commit2\n" +
                "        \n" +
                "        */\n" +
                "        msg.temperature = 4.0;\n" +
                "        break;\n" +
                "        // case:;\n" +
                "    case 15.0:\n" +
                "        msg.temperature = 3.3;\n" +
                "        break;\n" +
                "        // default:;\n" +
                "    default:\n" +
                "        msg.temperature = msg.temperature;\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        Double dd = 3.3;
        expected.put("temp", dd);
        Object actual = executeScript(scriptBodyTestSwitchNodeStr);
        assertEquals(expected, actual);
    }

    public void testSwitchNodeParameterNumberAsStringQuotes() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 120.0;\n" +
                "var par = \"203\";\n" +
                "\n" +
                "switch (par){\n" +
                "    case \"test\":\n" +
                "         msg.temperature = 1.0;\n" +
                "         break;\n" +
                "    case 203:\n" +
                "        msg.temperature = 2.0;\n" +
                "        break;\n" +
                "    case 15.0:\n" +
                "        msg.temperature = 3;\n" +
                "        break;\n" +
                "    default:\n" +
                "        msg.temperature = 4;\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        Double dd = 2.0;
        expected.put("temp", dd);
        Object actual = executeScript(scriptBodyTestSwitchNodeStr);
        assertEquals(expected, actual);
    }

    public void testSwitchNodeWithoutDefault() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 115.0;\n" +
                "var a = 25;\n" +
                "\n" +
                "if (msg.temperature === 19){\n" +
                "    msg.temperature = 19;\n" +
                "} else if (msg.temperature === 24){\n" +
                "    msg.temperature = 23.11;\n" +
                "}  else {\n" +
                "    msg.temperature = msg.temperature;\n" +
                "}\n" +
                "\n" +
                "switch (msg.temperature){\n" +
                "    case 115.0:\n" +
                "    case 10.0:\n" +
                "         switch (msg.temperature){\n" +
                "            case 122.4:\n" +
                "            case 10.0:\n" +
                "                 msg.temperature = 130.1;\n" +
                "                 break;\n" +
                "            case 112.0:\n" +
                "                msg.temperature = 101;\n" +
                "                break;\n" +
                "            case 115.0:\n" +
                "                msg.temperature = 105.6789;\n" +
                "                msg.temperature += a;\n" +
                "                msg.temperature = msg.temperature/2;\n" +
                "        }\n" +
                "         break;\n" +
                "    case 12.0:\n" +
                "        msg.temperature = 1;\n" +
                "        break;\n" +
                "    case 15.0:\n" +
                "        msg.temperature = 5;\n" +
                "        break;\n" +
                "    default:\n" +
                "        msg.temperature = 2;\n" +
                "\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        Double dd = (105.6789 + 25)/2;
        expected.put("temp", dd);
        Object actual = executeScript(scriptBodyTestSwitchNodeStr);
        assertEquals(expected, actual);
    }

    public void testSwitchNodeParameterStringWithoutEscapingQuotes_Error() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 120.0;\n" +
                "var par = \"test1\";\n" +
                "\n" +
                "switch (par){\n" +
                "    case test:\n" +
                "         msg.temperature = 1.0;\n" +
                "         break;\n" +
                "    case \"test1\":\n" +
                "        msg.temperature = 2.0;\n" +
                "        break;\n" +
                "    case 15.0:\n" +
                "        msg.temperature = 3;\n" +
                "        break;\n" +
                "    default:\n" +
                "        msg.temperature = 4;\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        LinkedHashMap<String, Double> expected = new LinkedHashMap<>();
        Double dd = 2.0;
        expected.put("temp", dd);
        try {
            Object actual = executeScript(scriptBodyTestSwitchNodeStr);
            assertEquals(expected, actual);
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: test"));
        }
    }

    public void testSwitchNodeWithout_Bracket_Error() {
        try {
            String scriptBodyTestSwitchNodeStr = "\n" +
                    "var msg = {};\n" +
                    "msg[\"temperature\"] = 100.0;\n" +
                    "var a = 25;\n" +
                    "\n" +
                    "switch msg.temperature){\n" +
                    "    case 115.0:\n" +
                    "    case 100.0:\n" +
                    "        msg.temperature = 1;\n" +
                    "        break;\n" +
                    "    case 12.0:\n" +
                    "        msg.temperature = 2;\n" +
                    "        break;\n" +
                    "    case 15.0:\n" +
                    "        msg.temperature = 5;\n" +
                    "        break;\n" +
                    "    default:\n" +
                    "        msg.temperature = 6;\n" +
                    "}\n" +
                    "return {temp: msg.temperature};\n";
            executeScript(scriptBodyTestSwitchNodeStr);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("Switch without expression or not find start/end of switch block"));
        }
    }

    public void testSwitchNodeWithout_Brace_Error() {
        try {
            String scriptBodyTestSwitchNodeStr = "\n" +
                    "var msg = {};\n" +
                    "msg[\"temperature\"] = 100.0;\n" +
                    "var a = 25;\n" +
                    "\n" +
                    "switch (msg.temperature) \n" +
                    "    case 115.0:\n" +
                    "    case 100.0:\n" +
                    "        msg.temperature = 1;\n" +
                    "        break;\n" +
                    "    case 12.0:\n" +
                    "        msg.temperature = 2;\n" +
                    "        break;\n" +
                    "    case 15.0:\n" +
                    "        msg.temperature = 5;\n" +
                    "        break;\n" +
                    "    default:\n" +
                    "        msg.temperature = 6;\n" +
                    "}\n" +
                    "return {temp: msg.temperature};\n";
            executeScript(scriptBodyTestSwitchNodeStr);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("Switch without expression or not find start/end of switch block"));
        }
    }

    public void testSwitchNodeWithout_BraceClose_Error() {
        try {
            String scriptBodyTestSwitchNodeStr = "\n" +
                    "var msg = {};\n" +
                    "msg[\"temperature\"] = 100.0;\n" +
                    "var a = 25;\n" +
                    "\n" +
                    "switch (msg.temperature) {\n" +
                    "    case 115.0:\n" +
                    "    case 100.0:\n" +
                    "        msg.temperature = 1;\n" +
                    "        break;\n" +
                    "    case 12.0:\n" +
                    "        msg.temperature = 2;\n" +
                    "        break;\n" +
                    "    case 15.0:\n" +
                    "        msg.temperature = 5;\n" +
                    "        break;\n" +
                    "    default:\n" +
                    "        msg.temperature = 6;\n" +
                    "\n" +
                    "return {temp: msg.temperature};\n";
            executeScript(scriptBodyTestSwitchNodeStr);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unbalanced braces { ... }"));
        }
    }

    public void testSwitchNodeFailedDefault_Error() {
        try {
            String scriptBodyTestSwitchNodeStr = "\n" +
                    "var msg = {};\n" +
                    "msg[\"temperature\"] = 100.0;\n" +
                    "var a = 25;\n" +
                    "\n" +
                    "switch (msg.temperature) {\n" +
                    "    case 115.0:\n" +
                    "    case 100.0:\n" +
                    "        msg.temperature = 1;\n" +
                    "        break;\n" +
                    "    case 12.0:\n" +
                    "        msg.temperature = 2;\n" +
                    "        break;\n" +
                    "    case 15.0:\n" +
                    "        msg.temperature = 5;\n" +
                    "        break;\n" +
                    "    default r :\n" +
                    "        msg.temperature = 6;\n" +
                    "}\n" +
                    "return {temp: msg.temperature};\n";
            executeScript(scriptBodyTestSwitchNodeStr);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("after \"default\" expected ':' but encountered: r"));
        }
    }
    public void testSwitchNodeWithoutCaseWithDefault_Error() {
        try {
            String scriptBodyTestSwitchNodeStr = "\n" +
                    "var msg = {};\n" +
                    "msg[\"temperature\"] = 100.0;\n" +
                    "var a = 25;\n" +
                    "\n" +
                    "switch (msg.temperature) {\n" +
                    "    default:\n" +
                    "        msg.temperature = 6;\n" +
                    "}\n" +
                    "return {temp: msg.temperature};\n";
            executeScript(scriptBodyTestSwitchNodeStr);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("statement expected"));
        }
    }

    public void testSwitchNodeEmpty_Error() {
        try {
            String scriptBodyTestSwitchNodeStr = "\n" +
                    "var msg = {};\n" +
                    "msg[\"temperature\"] = 100.0;\n" +
                    "var a = 25;\n" +
                    "\n" +
                    "switch (msg.temperature) {\n" +
                    "}\n" +
                    "return {temp: msg.temperature};\n";
            executeScript(scriptBodyTestSwitchNodeStr);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("Switch without expression or not find start/end of switch block"));
        }
    }
    public void testSwitchNodeWithoutCaseAndDefault_Error() {
        try {
            String scriptBodyTestSwitchNodeStr = "\n" +
                    "var msg = {};\n" +
                    "msg[\"temperature\"] = 100.0;\n" +
                    "var a = 25;\n" +
                    "\n" +
                    "switch (msg.temperature) {\n" +
                    "rty\n" +
                    "}\n" +
                    "return {temp: msg.temperature};\n";
            executeScript(scriptBodyTestSwitchNodeStr);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("statement expected"));
        }
    }
    public void testSwitchNode_CaseWithoutSwitch_Error() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 120;\n" +
                "case (msg.temperature){\n" +
                "    case 120.0:\n" +
                "        msg.temperature = 1.0;\n" +
                "         break;\n" +
                "    case 241:\n" +
                "        msg.temperature = 2.0;\n" +
                "        break;\n" +
                "    default:\n" +
                "        msg.temperature = 3.0;\n" +
                "\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        try{
            executeScript(scriptBodyTestSwitchNodeStr);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("case without switch"));
        }
    }
    public void testSwitchNodeDefaultWithoutCase_Error() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 120;\n" +
                "switch (msg.temperature){\n" +
                "    default:\n" +
                "        msg.temperature = 3.0;\n" +
                "\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        try{
            executeScript(scriptBodyTestSwitchNodeStr);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("statement expected"));
        }
    }
    public void testSwitchNodeDefaultWithoutSwitch_Error() {
        String scriptBodyTestSwitchNodeStr = "\n" +
                "var msg = {};\n" +
                "msg[\"temperature\"] = 120;\n" +
                "default (msg.temperature){\n" +
                "    case 120.0:\n" +
                "        msg.temperature = 1.0;\n" +
                "         break;\n" +
                "    default:\n" +
                "        msg.temperature = 3.0;\n" +
                "\n" +
                "}\n" +
                "return {temp: msg.temperature};\n";
        try{
            executeScript(scriptBodyTestSwitchNodeStr);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("default without switch"));
        }
    }

    public void testForWithBreakInIf_OnlyBreak() {
        String scriptBodyTestForWithBreakInIfStr =
                "var y = 0;\n" +
                "for (int i =0; i< 100; i++) {\n" +
                "        y=i;\n" +
                "        if (i > 2) {\n" +
                "            break;\n" +
                "        }\n" +
                "    }\n" +
                "return {\n" +
                "    msg: y\n" +
                "};" ;
        LinkedHashMap<String, Integer> expected = new LinkedHashMap<>();
        expected.put("msg", 3);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }
    public void testForWithBreakInIf_Function() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [1, 2, 3, 4];\n" +
                "var output = 10;\n" +
                "forBreak();\n" +
                "function forBreak() {\n" +
                "    for (var i = 0; i < input.size; i++) {\n" +
                "        output = i;\n" +
                "        if (i === 1) {\n" +
                "            output = input[i];\n" +
                "            break;\n" +
                "        }\n" +
                "        output = i;\n" +
                "    }\n" +
                "}" +
                 "return {\n" +
                "    msg: output\n" +
                "};\n" ;
        LinkedHashMap<String, Integer> expected = new LinkedHashMap<>();
        expected.put("msg", 2);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testForWithBreakOneFor() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [-1, -7, -3, -4];\n" +
                "var output = 9;\n" +
                "for (var i = 0; i < input.size; i++) {\n" +
                "    output = i * 10;\n" +
                "    if (i === 3) {\n" +
                "        output = input[i];\n" +
                "        break;\n" +
                "        output = i * 100;\n" +
                "    }\n" +
                "    output = i * 1000;\n" +
                "}\n" +
                "    output = output * 10000;\n" +
                "return {msg: output};";
        LinkedHashMap<String, Integer> expected = new LinkedHashMap<>();
        expected.put("msg", -40000);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testForWithBreakIncludesForWithBreak() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [-1, -7, -3, -4];\n" +
                "var output = 9;\n" +
                "var outputY = 19;\n" +
                "for (var i = 0; i < input.size; i++) {\n" +
                "      output = i*10;\n" +
                "    if (i === 2) {\n" +
                "      output = input[i];\n" +
                "      break;\n" +
                "      output = i*100;\n" +
                "    } else if ( i === 0) {\n" +
                "      outputY = 19;\n" +
                "      for (var y = 0; y < input.size; y++) {\n" +
                "        outputY = y*10*2;\n" +
                "        if (y === 1) {\n" +
                "          outputY = input[y];\n" +
                "          break;\n" +
                "          outputY = y*100*2;\n" +
                "        }\n" +
                "        outputY = y*1000*2;\n" +
                "      }\n" +
                "    }\n" +
                "    output = i*1000;\n" +
                "}\n" +
                "return {msg: [output, outputY]};";
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(-3);
        expIntList.add(-7);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testForWithBreakWithIncrementIndex() {
        String scriptBodyTestSwitchNodeStr = "var input = [0x02, 0x75, 45, 0x01, 0x75, 55, 0x03, 0x76,  75];\n" +
                "var output = { \"telemetry\": {\"battery\": 130}};\n" +
                "for (var i = 0; i < input.size;) {\n" +
                "        var channel_id = input[i++];\n" +
                "        var channel_type = input[i++];\n" +
                "        // BATTERY\n" +
                "        if (channel_id === 0x01 && channel_type === 0x75) {\n" +
                "            output.telemetry.battery = input[i];\n" +
                "            break;\n" +
                "        }\n" +
                "        i += 1;\n" +
                "\n" +
                "}\n" +
                "\n" +
                "return {msg: output.telemetry.battery};";
        LinkedHashMap<String, Integer> expected = new LinkedHashMap<>();
        expected.put("msg", 55);
        Object actual = executeScript(scriptBodyTestSwitchNodeStr);
        assertEquals(expected, actual);
    }

    public void testForeachWithBreakIncludesForeachWithBreak() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [-1, -10, -3, -4];\n" +
                        "var output = 9;\n" +
                        "var outputY = 19;\n" +
                        "var i = 0;\n" +
                        "var y = 0;\n" +
                        "foreach(a: input) {\n" +
                        "    output = i * 10;\n" +
                        "    if (i === 2) {\n" +
                        "        output = a;\n" +
                        "        break;\n" +
                        "        output = i * 100;\n" +
                        "    } else if (i === 0) {\n" +
                        "        outputY = 19;\n" +
                        "        y = 0;\n" +
                        "        foreach(b: input) {\n" +
                        "            outputY = y * 10 * 2;\n" +
                        "            if (y === 1) {\n" +
                        "                outputY = b;\n" +
                        "                break;\n" +
                        "                outputY = y * 100 * 2;\n" +
                        "            }\n" +
                        "            outputY = y * 1000 * 2;\n" +
                        "            y++;\n" +
                        "        }\n" +
                        "    }\n" +
                        "    output = i * 1000;\n" +
                        "    i++;\n" +
                        "}\n" +
                        "output = output * 4;\n" +
                        "outputY = outputY / 2;\n" +
                        "return {\n" +
                        "    msg: [output, outputY, i, y]\n" +
                        "};";
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(-12);
        expIntList.add(-5);
        expIntList.add(2);
        expIntList.add(1);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testForeachWithBreakInIf_Function() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [-1, -2, -3, -4];\n" +
                "var output = 10;\n" +
                "var i = 0;\n" +
                "forBreak();\n" +
                "function forBreak() {\n" +
                "    foreach(a: input) {\n" +
                "        output = i;\n" +
                "        if (i === 1) {\n" +
                "            output = a;\n" +
                "            break;\n" +
                "        }\n" +
                "        output = i;\n" +
                "        i++;\n" +
                "    }\n" +
                "}" +
                "return {\n" +
                "    msg: output\n" +
                "};\n" ;
        LinkedHashMap<String, Integer> expected = new LinkedHashMap<>();
        expected.put("msg",-2);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testWhileWithBreak() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [-1, -7, -3, -4];\n" +
                "var output = -9;\n" +
                "var i = 0;\n" +
                "while (i < input.size()) {\n" +
                   "    output = i * 10;\n" +
                "    if (i === 1) {\n" +
                "        output = input[i];\n" +
                "        break;\n" +
                "        output = i * 100;\n" +
                "    }\n" +
                "    i++;\n" +
                "    output = i * 1000;\n" +
                "}\n" +
                "output = output * 4;\n" +
                "return {\n" +
                "    msg: [output, i]\n" +
                "};";
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(-28);
        expIntList.add(1);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testWhileWithBreakIncludesWhileWithBreak() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [-1, -10, -3, -4];\n" +
                        "var output = -9;\n" +
                        "var outputY = -19;\n" +
                        "var i = 0;\n" +
                        "var y = 0;\n" +
                        "while (i < input.size()) {\n" +
                        "    output = i * 10;\n" +
                        "    if (i === 2) {\n" +
                        "        output = input[i];\n" +
                        "        break;\n" +
                        "        output = i * 100;\n" +
                        "    } else if (i === 0) {\n" +
                        "        outputY = -20;\n" +
                        "        y = 0;\n" +
                        "       while (y < input.size()) {\n" +
                        "            outputY = y * 10 * 2;\n" +
                        "            if (y === 1) {\n" +
                        "                outputY = input[y];\n" +
                        "                break;\n" +
                        "                outputY = y * 100 * 2;\n" +
                        "            }\n" +
                        "            outputY = y * 1000 * 2;\n" +
                        "            y++;\n" +
                        "        }\n" +
                        "    }\n" +
                        "    output = i * 1000;\n" +
                        "    i++;\n" +
                        "}\n" +
                        "output = output * 4;\n" +
                        "outputY = outputY / 2;\n" +
                        "return {\n" +
                        "    msg: [output, outputY, i, y]\n" +
                        "};";
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(-12);
        expIntList.add(-5);
        expIntList.add(2);
        expIntList.add(1);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testWhileWithBreakInIf_Function() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [-1, -2, -3, -4];\n" +
                        "var output = 10;\n" +
                        "var i = 0;\n" +
                        "forBreak();\n" +
                        "function forBreak() {\n" +
                        "    while (i < input.size()) {\n" +
                        "        output = i;\n" +
                        "        if (i === 1) {\n" +
                        "            output = input[i];\n" +
                        "            break;\n" +
                        "        }\n" +
                        "        output = i;\n" +
                        "        i++;\n" +
                        "    }\n" +
                        "}" +
                        "return {\n" +
                        "    msg: [output, i]\n" +
                        "};\n" ;
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(-2);
        expIntList.add(1);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testDoWithBreak() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [-1, -7, -3, -4];\n" +
                "var output = -9;\n" +
                "var i = 0;\n" +
                "do {\n" +
                   "    output = i * 10;\n" +
                "    if (i === 1) {\n" +
                "        output = input[i];\n" +
                "        break;\n" +
                "        output = i * 100;\n" +
                "    }\n" +
                "    i++;\n" +
                "    output = i * 1000;\n" +
                "}\n" +
                "while (i < input.size()) \n" +
                "output = output * 4;\n" +
                "return {\n" +
                "    msg: [output, i]\n" +
                "};";
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(-28);
        expIntList.add(1);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testDoWithBreakIncludesDoWithBreak() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [-1, -10, -3, -4];\n" +
                        "var output = -9;\n" +
                        "var outputY = -19;\n" +
                        "var i = 0;\n" +
                        "var y = 0;\n" +
                        "do {\n" +
                        "    output = i * 10;\n" +
                        "    if (i === 2) {\n" +
                        "        output = input[i];\n" +
                        "        break;\n" +
                        "        output = i * 100;\n" +
                        "    } else if (i === 0) {\n" +
                        "        outputY = -20;\n" +
                        "        y = 0;\n" +
                        "       do {\n" +
                        "            outputY = y * 10 * 2;\n" +
                        "            if (y === 1) {\n" +
                        "                outputY = input[y];\n" +
                        "                break;\n" +
                        "                outputY = y * 100 * 2;\n" +
                        "            }\n" +
                        "            outputY = y * 1000 * 2;\n" +
                        "            y++;\n" +
                        "        }\n" +
                        "       while (y < input.size()) \n" +
                        "    }\n" +
                        "    output = i * 1000;\n" +
                        "    i++;\n" +
                        "}\n" +
                        "while (i < input.size()) \n" +
                        "output = output * 4;\n" +
                        "outputY = outputY / 2;\n" +
                        "return {\n" +
                        "    msg: [output, outputY, i, y]\n" +
                        "};";
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(-12);
        expIntList.add(-5);
        expIntList.add(2);
        expIntList.add(1);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testDoWithBreakInIf_Function() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [-1, -2, -3, -4];\n" +
                        "var output = 10;\n" +
                        "var i = 0;\n" +
                        "forBreak();\n" +
                        "function forBreak() {\n" +
                        "   do {\n" +
                        "        output = i;\n" +
                        "        if (i === 1) {\n" +
                        "            output = input[i];\n" +
                        "            break;\n" +
                        "        }\n" +
                        "        output = i;\n" +
                        "        i++;\n" +
                        "    }\n" +
                        "    while (i < input.size()) \n" +
                        "}" +
                        "return {\n" +
                        "    msg: [output, i]\n" +
                        "};\n" ;
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(-2);
        expIntList.add(1);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testDoUntilWithBreak() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [-1, -7, -3, -4];\n" +
                "var output = -9;\n" +
                "var i = 0;\n" +
                "do {\n" +
                "    output = i * 10;\n" +
                "    if (i === 2) {\n" +
                "        output = input[i];\n" +
                "        break;\n" +
                "        output = i * 100;\n" +
                "    }\n" +
                "    i++;\n" +
                "    output = i * 1000;\n" +
                "}\n" +
                "until (i > input.size()) \n" +
                "output = output * 4;\n" +
                "return {\n" +
                "    msg: [output, i]\n" +
                "};";
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(-12);
        expIntList.add(2);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testDoUntilWithBreakIncludesDoWithBreak() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [-1, -7, -3, -4];\n" +
                "var output = -9;\n" +
                "var outputY = -19;\n" +
                "var i = 0;\n" +
                "var y = 0;\n" +
                "do {\n" +
                "    output = i * 10;\n" +
                "    if (i === 2) {\n" +
                "        output = input[i];\n" +
                "        break;\n" +
                "        output = i * 100;\n" +
                "    } else if (i === 0) {\n" +
                "        outputY = -20;\n" +
                "        y = 0;\n" +
                "       do {\n" +
                "            outputY = y * 10 * 2;\n" +
                "            if (y === 1) {\n" +
                "                outputY = input[y];\n" +
                "                break;\n" +
                "                outputY = y * 100 * 2;\n" +
                "            }\n" +
                "            outputY = y * 1000 * 2;\n" +
                "            y++;\n" +
                "       }\n" +
                "       until (y > input.size()) \n" +
                "    }\n" +
                "    i++;\n" +
                "    output = i * 1000;\n" +
                "}\n" +
                "until (i > input.size()) \n" +
                "output = output * 4;\n" +
                "return {\n" +
                "    msg: [output, outputY, i, y]\n" +
                "};";
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(-12);
        expIntList.add(-7);
        expIntList.add(2);
        expIntList.add(1);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testDoUntilWithBreakInIf_Function() {
        String scriptBodyTestForWithBreakInIfStr =
                "var input = [-1, -2, -3, -4];\n" +
                        "var output = 10;\n" +
                        "var i = 0;\n" +
                        "forBreak();\n" +
                        "function forBreak() {\n" +
                        "   do {\n" +
                        "        output = i;\n" +
                        "        if (i === 1) {\n" +
                        "            output = input[i];\n" +
                        "            break;\n" +
                        "        }\n" +
                        "        output = i;\n" +
                        "        i++;\n" +
                        "    }\n" +
                        "    until (i > input.size()) \n" +
                        "}" +
                        "return {\n" +
                        "    msg: [output, i]\n" +
                        "};\n" ;
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(-2);
        expIntList.add(1);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testForVar_a_FunctionWithForVar_a() {
        String scriptBodyTestForWithBreakInIfStr =
                "var output = 0;\n" +
                "for (var a = 0; a < 10; a++) {\n" +
                "    output = testBreak(output);\n" +
                "}\n" +
                "return {\n" +
                "    msg: [output]\n" +
                "};\n" +
                "function testBreak(val) {\n" +
                "    for (var a = 0; a< 9; a++) {\n" +
                "        val++;\n" +
                "    }\n" +
                "    return val;\n" +
                "}" +
                "\n" ;
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(90);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testLeve0ForVar_a_Level0_a_unresolvable() {
        String scriptBodyTestForWithBreakInIfStr =
                "var output = 0;\n" +
                "for (var a = 0; a < 10; a++) {\n" +
                "}\n" +
                "output = a;\n" +
                "return {\n" +
                "    msg: [output]\n" +
                "};\n" +
                "\n" ;
        try {
            executeScript(scriptBodyTestForWithBreakInIfStr);
            fail("Should throw PropertyAccessException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unresolvable property or identifier: a"));
        }

    }

    /**
     * Fix_bug:
     * - Script with `For` level0 (parameter name 'a')
     * - Function level0 with  `For` (parameter name 'a')
     * - condition_number_1 (`For` level0 parameter 'a')  IS EQUAL TO OR less than condition_number_2 (`For` level0 parameter 'a') by +1
     * - in Function result: "unable to resolve variable 'a'"
     */
    public void testLeve0ForVar_a_And_FunctionWithForVar_a_Function_Calling_Level0() {
        String scriptBodyTestForWithBreakInIfStr =
                "var output = 0;\n" +
                "output = testBreak(output);\n" +
                "for (var a = 0; a < 10; a++) {\n" +
                "}\n" +
                "for (var y = 0; y < 2; y++) {\n" +
                "        output++;\n" +
                "}\n" +
                "for (var r = 0; r < 10; r++) {\n" +
                "}\n" +
                "return {\n" +
                "    msg: [output]\n" +
                "};\n" +
                "function testBreak(val) {\n" +
                "    var b = 45;\n" +
                "    for (var r = 0; r < 5; r++) {\n" +
                "        output++;\n" +
                "    }\n" +
                "    val = output;\n" +
                "    for (var a = 0; a < 9; a++) {\n" +
                "        val++;\n" +
                "    }\n" +
                "    return val;\n" +
                "}" +
                "\n" ;
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(16);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    /**
     * Fix_bug:
     * - Script with `For` level0 (parameter name 'a')
     * - Function level0 with  `For` (parameter name 'a')
     * - condition_number_1 (`For` level0 parameter 'a')  is greater than condition_number_2 (`For` level0 parameter 'a') by more than +2
     * - in Function with For result: "Infinite Loops For" -> Should throw ScriptExecutionStoppedException
     */
    public void testOneNameVar_In_Another_Procedure_NotTimeout() {
        String scriptBodyTestForWithBreakInIfStr =
                "var output = 0;\n" +
                "for (var a = 0; a < 100; a++) {\n" +
                "    output = testBug(output);\n" +
                "}\n" +
                "output = testBug(output);\n" +
                "return {\n" +
                "    msg: [output]\n" +
                "};\n" +
                "function testBug(val) {\n" +
                "    for (var a = 0; a < 9; a++) {\n" +
                "        val++;\n" +
                "    }\n" +
                "    return val;\n" +
                "}" ;
        LinkedHashMap<String, ArrayList<Integer>> expected = new LinkedHashMap<>();
        ArrayList<Integer> expIntList = new ArrayList<>();
        expIntList.add(909);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBodyTestForWithBreakInIfStr);
        assertEquals(expected, actual);
    }

    public void testInnerFunctionForReturn() {
        String body = "var output = 0;\n" +
                "\n" +
                "output = testBug(output);\n" +
                "\n" +
                "return output;\n" +
                "\n" +
                "function testBug(val) {\n" +
                "    for (var i=0 ;i<10;i++) {\n" +
                "        return 1;\n" +
                "    }\n" +
                "    return 2;\n" +
                "}";
        Object result = executeScript(body);
        assertTrue(result instanceof Integer);
        assertEquals(1, result);
    }

    public void testInnerFunctionForConditionalReturn() {
        String body = "var output = 0;\n" +
                "\n" +
                "output = testBug(output);\n" +
                "\n" +
                "return output;\n" +
                "\n" +
                "function testBug(val) {\n" +
                "    for (var i=0 ;i<10;i++) {\n" +
                "        if (i > 5) {\n" +
                "             return i;\n" +
                "        }\n" +
                "    }\n" +
                "    return 2;\n" +
                "}";
        Object result = executeScript(body);
        assertTrue(result instanceof Integer);
        assertEquals(6, result);
    }

   public void testInnerFunctionWhileReturn() {
        String body = "var output = 0;\n" +
                "\n" +
                "output = testBug(output);\n" +
                "\n" +
                "return output;\n" +
                "\n" +
                "function testBug(val) {\n" +
                "    var i = 0;              \n" +
                "    while ( i < 10) {\n" +
                "        return 1;\n" +
                "    }\n" +
                "    return 2;\n" +
                "}";
        Object result = executeScript(body);
        assertTrue(result instanceof Integer);
        assertEquals(1, result);
    }

    public void testInnerFunctionWhileConditionalReturn() {
        String body = "var output = 0;\n" +
                "\n" +
                "output = testBug(output);\n" +
                "\n" +
                "return output;\n" +
                "\n" +
                "function testBug(val) {\n" +
                "    var i = 0;              \n" +
                "    while ( i < 10) {\n" +
                "        if (i > 5) {\n" +
                "           return i;\n" +
                "        }\n" +
                "        i++;" +
                "    }\n" +
                "    return 2;\n" +
                "}";
        Object result = executeScript(body);
        assertTrue(result instanceof Integer);
        assertEquals(6, result);
    }

    public void testIntegerToLongFromJson() {
        Integer sunriseValueOld = 1695435081;
        Long sunriseValueNew = Long.valueOf(sunriseValueOld) * 1000;
        String sunriseName = "sunrise";
        LinkedHashMap<String, LinkedHashMap> vars = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> msg = new LinkedHashMap<>();

        msg.put("sys", sunriseValueOld);
        vars.put("msg", msg);
        String body = "var time = msg.sys * 1000;\n" +
                "msg."  + sunriseName +  " = time;\n" +
                "return {\"msg\": msg};";
        Object actual = executeScript(body, vars);

        LinkedHashMap<String, LinkedHashMap> expected = vars;
        expected.get("msg").put(sunriseName, sunriseValueNew);
        assertEquals(expected, actual);
    }
    public void testIntegerToIntegerFromJson() {
        Integer sunriseValueOld = 169543;
        Integer sunriseValueNew = sunriseValueOld * 10;
        String sunriseName = "sunrise";
        LinkedHashMap<String, LinkedHashMap> vars = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> msg = new LinkedHashMap<>();

        msg.put("sys", sunriseValueOld);
        vars.put("msg", msg);
        String body = "var time = msg.sys * 10;\n" +
                "msg."  + sunriseName +  " = time;\n" +
                "return {\"msg\": msg};";
        Object actual = executeScript(body, vars);

        LinkedHashMap<String, LinkedHashMap> expected = vars;
        expected.get("msg").put(sunriseName, sunriseValueNew);
        assertEquals(expected, actual);
    }
    public void testIntegerAsObjectToLongFromJson_Ok() {
        Integer sunriseValueOld = 1695435081;
        Long sunriseValueNew = Long.valueOf(sunriseValueOld) * 1000;
        String sunriseName = "sunrise";
        LinkedHashMap<String, LinkedHashMap> vars = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> msg = new LinkedHashMap<>();

        msg.put("sys", (Integer) sunriseValueOld);
        vars.put("msg", msg);
        String body = "var time = " + sunriseValueOld + " * 1000;\n" +
                "msg."  + sunriseName +  " = time;\n" +
                "return {\"msg\": msg};";
        Object actual = executeScript(body, vars);

        LinkedHashMap<String, LinkedHashMap> expected = vars;
        expected.get("msg").put(sunriseName, sunriseValueNew);
        assertEquals(expected, actual);
    }
    public void testIntegerAsObjectToLongFromJson_If_result_less_Integer_MIN_VALUE() {
        Integer sunriseValueOld = -1695435081;
        Long sunriseValueNew = Long.valueOf(sunriseValueOld) * -1000;
        String sunriseName = "sunrise";
        LinkedHashMap<String, LinkedHashMap> vars = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> msg = new LinkedHashMap<>();

        msg.put("sys", (Integer) sunriseValueOld);
        vars.put("msg", msg);
        String body = "var time = " + sunriseValueOld + " * -1000;\n" +
                "msg."  + sunriseName +  " = time;\n" +
                "return {\"msg\": msg};";
        Object actual = executeScript(body, vars);

        LinkedHashMap<String, LinkedHashMap> expected = vars;
        expected.get("msg").put(sunriseName, sunriseValueNew);
        assertEquals(expected, actual);
    }

   public void testIntegerToIntegerFromJson_If_result_less_Integer_MIN_VALUE() {

        Integer sunriseValueOld = -1695435081;
        Long sunriseValueNew = Long.valueOf(sunriseValueOld) * 10000;
        String sunriseName = "sunrise";
        LinkedHashMap<String, LinkedHashMap> vars = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> msg = new LinkedHashMap<>();

        msg.put("sys", sunriseValueOld);
       vars.put("msg", msg);
       String body = "var time = msg.sys * 10000;\n" +
               "msg." + sunriseName + " = time;\n" +
               "return {\"msg\": msg};";
       Object actual = executeScript(body, vars);

       LinkedHashMap<String, LinkedHashMap> expected = vars;
       expected.get("msg").put(sunriseName, sunriseValueNew);
       assertEquals(expected, actual);
   }

    public void testExecutionArrayListToString() {
        String body = "var list = ['hello', 34567];\n" +
                      "var res = '' + list;\n" +
                      "return res;";
        Object result = executeScript(body);
        assertTrue(result instanceof String);
        assertEquals("[hello, 34567]", result);
    }

    public void testExecutionArrayListJoin() {
        String body = "var list = [];\n" +
                "return list.join();";
        Object result = executeScript(body);
        assertTrue(result instanceof String);
        assertEquals("", result);
        body = "var list = ['hello', 34567];\n" +
                "return list.join();";
        result = executeScript(body);
        assertTrue(result instanceof String);
        assertEquals("hello,34567", result);
        body = "var list = ['hello', 34567];\n" +
                "return list.join(':');";
        result = executeScript(body);
        assertTrue(result instanceof String);
        assertEquals("hello:34567", result);
        body = "var matrix = [\n" +
                "  [1, 2, \"Things\"],\n" +
                "  [4, 5, 6],\n" +
                "  245, \"Test_Join\",\n" +
                "  [7, \"8\", 9],\n" +
                "];\n" +
                "return matrix.join();";
        result = executeScript(body);
        assertTrue(result instanceof String);
        assertEquals("1,2,Things,4,5,6,245,Test_Join,7,8,9", result);
    }

    public void testExecutionHashMapToString() {
        String body = "var map = {hello: 'world', testmap: 'toString'};\n" +
                      "return '' + map;";
        Object result = executeScript(body);
        assertTrue(result instanceof String);
        assertEquals("{hello=world, testmap=toString}", result);
    }

    public void testExecutionHashMapKeys() {
        String body = "var map = {hello: 'world', testmap: 'toString'};\n" +
                      "return map.keys();";
        Object result = executeScript(body);
        assertTrue(result instanceof List);
        assertEquals(2, ((List)result).size());
        assertEquals("hello", ((List)result).get(0));
        assertEquals("testmap", ((List)result).get(1));
    }

    public void testExecutionHashMapValues() {
        String body = "var map = {hello: 'world', testmap: 'toString'};\n" +
                "return map.values();";
        Object result = executeScript(body);
        assertTrue(result instanceof List);
        assertEquals(2, ((List) result).size());
        assertEquals("world", ((List) result).get(0));
        assertEquals("toString", ((List) result).get(1));
    }

    public void testExecutionArrayListSortAsc() {
        String body = "var msg = {};\n" +
                "var arrayString = ['March', 'Feb', 'Jan', 'Dec'];\n" +
                "arrayString.sort();\n" +
                "var arrayInt = [1, 30, 4, -214748, 57, 214748, 100000];\n" +
                "arrayInt.sort();\n" +
                "var arrayLong = [45, 9223372036854775807, 30, 40,  -9223372036854775808, 1000];\n" +
                "arrayLong.sort();\n" +
                "var arrayFloat = [3.40282, 34.17549467, 45.40283, 1.1754943];\n" +
                "arrayFloat.sort();\n" +
                "var mixedNumericArray = [\"8\", \"9\", \"700\", 40, 1, 5, 200];\n" +
                "mixedNumericArray.sort();\n" +
                "msg.arrayString = arrayString;\n" +
                "msg.arrayInt = arrayInt;\n" +
                "msg.arrayLong = arrayLong;\n" +
                "msg.arrayFloat = arrayFloat;\n" +
                "msg.mixedNumericArray = mixedNumericArray;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        String[] arrayString = new String[]{"Dec", "Feb", "Jan", "March"};
        List expectedArrayString = Arrays.asList(arrayString);
        List actualArray = (List) resMap.get("arrayString");
        assertEquals(expectedArrayString, actualArray);

        Integer[] arrayInt = new Integer[]{-214748, 1, 4, 30, 57, 100000, 214748};
        List expectedArrayInteger = Arrays.asList(arrayInt);
        actualArray = (List) resMap.get("arrayInt");
        assertEquals(expectedArrayInteger, actualArray);

        List expectedArrayIntLong = new ArrayList();
        expectedArrayIntLong.add(-9223372036854775808L);
        expectedArrayIntLong.add(30);
        expectedArrayIntLong.add(40);
        expectedArrayIntLong.add(45);
        expectedArrayIntLong.add(1000);
        expectedArrayIntLong.add(9223372036854775807L);
        actualArray = (List) resMap.get("arrayLong");
        assertEquals(expectedArrayIntLong, actualArray);

        Double[] arrayFloat = new Double[]{1.1754943, 3.40282, 34.17549467, 45.40283};
        List expectedArrayFloat = Arrays.asList(arrayFloat);
        actualArray = (List) resMap.get("arrayFloat");
        assertEquals(expectedArrayFloat, actualArray);

        List expectedArrayMixedNumeric = new ArrayList();
        expectedArrayMixedNumeric.add(1);
        expectedArrayMixedNumeric.add(5);
        expectedArrayMixedNumeric.add("8");
        expectedArrayMixedNumeric.add("9");
        expectedArrayMixedNumeric.add(40);
        expectedArrayMixedNumeric.add(200);
        expectedArrayMixedNumeric.add("700");
        actualArray = (List) resMap.get("mixedNumericArray");
        assertEquals(expectedArrayMixedNumeric, actualArray);
    }

    public void testExecutionArrayListSortDesc() {
        String body = "var msg = {};\n" +
                "var arrayString = ['March', 'Feb', 'Jan', 'Dec'];\n" +
                "arrayString.sort(false);\n" +
                "var arrayInt = [1, 30, 4, -214748, 57, 214748, 100000];\n" +
                "arrayInt.sort(false);\n" +
                "var arrayLong = [45, 9223372036854775807, 30, 40,  -9223372036854775808, 1000];\n" +
                "arrayLong.sort(false);\n" +
                "var arrayFloat = [3.40282, 34.17549467, 45.40283, 1.1754943];\n" +
                "arrayFloat.sort(false);\n" +
                "var mixedNumericArray = [\"8\", \"9\", \"700\", 40, 1, 5, 200];\n" +
                "mixedNumericArray.sort(false);\n" +
                "msg.arrayString = arrayString;\n" +
                "msg.arrayInt = arrayInt;\n" +
                "msg.arrayLong = arrayLong;\n" +
                "msg.arrayFloat = arrayFloat;\n" +
                "msg.mixedNumericArray = mixedNumericArray;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        String[] arrayString = new String[]{"Dec", "Feb", "Jan", "March"};
        List expectedArrayString = Arrays.asList(arrayString);
        Collections.reverse(expectedArrayString);
        List actualArray = (List) resMap.get("arrayString");
        assertEquals(expectedArrayString, actualArray);

        Integer[] arrayInt = new Integer[]{214748, 100000, 57, 30, 4, 1, -214748};
        List expectedArrayInteger = Arrays.asList(arrayInt);
        actualArray = (List) resMap.get("arrayInt");
        assertEquals(expectedArrayInteger, actualArray);

        List expectedArrayIntLong = new ArrayList();
        expectedArrayIntLong.add(-9223372036854775808L);
        expectedArrayIntLong.add(30);
        expectedArrayIntLong.add(40);
        expectedArrayIntLong.add(45);
        expectedArrayIntLong.add(1000);
        expectedArrayIntLong.add(9223372036854775807L);
        Collections.reverse(expectedArrayIntLong);
        actualArray = (List) resMap.get("arrayLong");
        assertEquals(expectedArrayIntLong, actualArray);

        Double[] arrayFloat = new Double[]{45.40283, 34.17549467, 3.40282, 1.1754943};
        List expectedArrayFloat = Arrays.asList(arrayFloat);
        actualArray = (List) resMap.get("arrayFloat");
        assertEquals(expectedArrayFloat, actualArray);

        List expectedArrayMixedNumeric = new ArrayList();
        expectedArrayMixedNumeric.add(1);
        expectedArrayMixedNumeric.add(5);
        expectedArrayMixedNumeric.add("8");
        expectedArrayMixedNumeric.add("9");
        expectedArrayMixedNumeric.add(40);
        expectedArrayMixedNumeric.add(200);
        expectedArrayMixedNumeric.add("700");
        Collections.reverse(expectedArrayMixedNumeric);
        actualArray = (List) resMap.get("mixedNumericArray");
        assertEquals(expectedArrayMixedNumeric, actualArray);
    }

    public void testExecutionArrayList_toSorted() {
        String body = "var msg = {};\n" +
                "var array = [\"8\", 40, 9223372036854775807, \"1234567\", \"-9223372036854775808\"];\n" +
                "var arraySort = array.toSorted();\n" +
                "msg.array = array;\n" +
                "msg.arraySort = arraySort;\n" +
                "return {msg: msg}";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add("8");
        expectedArray.add(40);
        expectedArray.add(9223372036854775807L);
        expectedArray.add("1234567");
        expectedArray.add("-9223372036854775808");
        List actualArray = (List) resMap.get("array");
        assertEquals(expectedArray, actualArray);
        actualArray = (List) resMap.get("arraySort");
        expectedArray.sort(numericCompAsc);
        assertEquals(expectedArray, actualArray);
    }

    public void testExecutionHashMapSortByValueAsc() {
        String body = "var msg = {};\n" +
                "var sortValString = {2:\"21Dragon\", 3:\"Brain\", 4:\"20\", 1:\"30\"};\n" +
                "sortValString.sortByValue();\n" +
                "var sortValInt = {2:1, 5:30, 225:4, 8:-214748, 1:57, 3:214748, 9:100000};\n" +
                "sortValInt.sortByValue();\n" +
                "var sortValIntLong = {2:45, 5:9223372036854775807, 225:30, 8:40,  3:-9223372036854775808, 9:1000};\n" +
                "sortValIntLong.sortByValue();\n" +
                "var sortValFloat = {2:3.40282, 225:34.17549467, 3:45.40283, 5:1.1754943};\n" +
                "sortValFloat.sortByValue();\n" +
                "var sortValMixedNumeric = {2:\"8\", 225:\"-9223372036854775808\", 3:\"700\", 5:40, 9:9223372036854775807, 4:5, 300:200};\n" +
                "sortValMixedNumeric.sortByValue();\n" +
                "\n" +
                "msg.sortValString = sortValString;\n" +
                "msg.sortValInt = sortValInt;\n" +
                "msg.sortValIntLong = sortValIntLong;\n" +
                "msg.sortValFloat = sortValFloat;\n" +
                "msg.sortValMixedNumeric = sortValMixedNumeric;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<Integer, String> expectedSortValString = new LinkedHashMap<>();
        expectedSortValString.put(4, "20");
        expectedSortValString.put(2, "21Dragon");
        expectedSortValString.put(1, "30");
        expectedSortValString.put(3, "Brain");
        LinkedHashMap actualHashMap = (LinkedHashMap) resMap.get("sortValString");
        assertEquals(expectedSortValString, actualHashMap);
        ArrayList expectedList = new ArrayList<Object>(expectedSortValString.values());
        ArrayList actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Integer> expectedSortValInt = new LinkedHashMap<>();
        expectedSortValInt.put(8, -214748);
        expectedSortValInt.put(2, 1);
        expectedSortValInt.put(225, 4);
        expectedSortValInt.put(5, 30);
        expectedSortValInt.put(1, 57);
        expectedSortValInt.put(9, 100000);
        expectedSortValInt.put(3, 214748);
        actualHashMap = (LinkedHashMap) resMap.get("sortValInt");
        assertEquals(expectedSortValInt, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortValInt.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Object> expectedSortValIntLong = new LinkedHashMap<>();
        expectedSortValIntLong.put(3, -9223372036854775808L);
        expectedSortValIntLong.put(225, 30);
        expectedSortValIntLong.put(8, 40);
        expectedSortValIntLong.put(2, 45);
        expectedSortValIntLong.put(9, 1000);
        expectedSortValIntLong.put(5, 9223372036854775807L);
        actualHashMap = (LinkedHashMap) resMap.get("sortValIntLong");
        assertEquals(expectedSortValIntLong, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortValIntLong.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Double> expectedSortValFloat = new LinkedHashMap<>();
        expectedSortValFloat.put(5, 1.1754943);
        expectedSortValFloat.put(2, 3.40282);
        expectedSortValFloat.put(225, 34.17549467);
        expectedSortValFloat.put(3, 45.40283);
        actualHashMap = (LinkedHashMap) resMap.get("sortValFloat");
        assertEquals(expectedSortValFloat, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortValFloat.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Object> expectedSortValMixedNumeric = new LinkedHashMap<>();
        expectedSortValMixedNumeric.put(225, "-9223372036854775808");
        expectedSortValMixedNumeric.put(4, 5);
        expectedSortValMixedNumeric.put(2, "8");
        expectedSortValMixedNumeric.put(5, 40);
        expectedSortValMixedNumeric.put(300, 200);
        expectedSortValMixedNumeric.put(3, "700");
        expectedSortValMixedNumeric.put(9, 9223372036854775807L);
        actualHashMap = (LinkedHashMap) resMap.get("sortValMixedNumeric");
        assertEquals(expectedSortValMixedNumeric, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortValMixedNumeric.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);
    }

    public void testExecutionHashMapSortByValueDesc() {
        String body = "var msg = {};\n" +
                "var sortValString = {2:\"21Dragon\", 3:\"Brain\", 4:\"20\", 1:\"30\"};\n" +
                "sortValString.sortByValue(false);\n" +
                "var sortValInt = {2:1, 5:30, 225:4, 8:-214748, 1:57, 3:214748, 9:100000};\n" +
                "sortValInt.sortByValue(false);\n" +
                "var sortValIntLong = {2:45, 5:9223372036854775807, 225:30, 8:40,  3:-9223372036854775808, 9:1000};\n" +
                "sortValIntLong.sortByValue(false);\n" +
                "var sortValFloat = {2:3.40282, 225:34.17549467, 3:45.40283, 5:1.1754943};\n" +
                "sortValFloat.sortByValue(false);\n" +
                "var sortValMixedNumeric = {2:\"8\", 225:\"-9223372036854775808\", 3:\"700\", 5:40, 9:9223372036854775807, 4:5, 300:200};\n" +
                "sortValMixedNumeric.sortByValue(false);\n" +
                "\n" +
                "msg.sortValString = sortValString;\n" +
                "msg.sortValInt = sortValInt;\n" +
                "msg.sortValIntLong = sortValIntLong;\n" +
                "msg.sortValFloat = sortValFloat;\n" +
                "msg.sortValMixedNumeric = sortValMixedNumeric;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<Integer, String> expectedSortValString = new LinkedHashMap<>();
        expectedSortValString.put(4, "20");
        expectedSortValString.put(2, "21Dragon");
        expectedSortValString.put(1, "30");
        expectedSortValString.put(3, "Brain");
        LinkedHashMap actualHashMap = (LinkedHashMap) resMap.get("sortValString");
        assertEquals(expectedSortValString, actualHashMap);
        ArrayList expectedList = new ArrayList<Object>(expectedSortValString.values());
        ArrayList actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Integer> expectedSortValInt = new LinkedHashMap<>();
        expectedSortValInt.put(8, -214748);
        expectedSortValInt.put(2, 1);
        expectedSortValInt.put(225, 4);
        expectedSortValInt.put(5, 30);
        expectedSortValInt.put(1, 57);
        expectedSortValInt.put(9, 100000);
        expectedSortValInt.put(3, 214748);
        actualHashMap = (LinkedHashMap) resMap.get("sortValInt");
        assertEquals(expectedSortValInt, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortValInt.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Object> expectedSortValIntLong = new LinkedHashMap<>();
        expectedSortValIntLong.put(3, -9223372036854775808L);
        expectedSortValIntLong.put(225, 30);
        expectedSortValIntLong.put(8, 40);
        expectedSortValIntLong.put(2, 45);
        expectedSortValIntLong.put(9, 1000);
        expectedSortValIntLong.put(5, 9223372036854775807L);
        actualHashMap = (LinkedHashMap) resMap.get("sortValIntLong");
        assertEquals(expectedSortValIntLong, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortValIntLong.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Double> expectedSortValFloat = new LinkedHashMap<>();
        expectedSortValFloat.put(5, 1.1754943);
        expectedSortValFloat.put(2, 3.40282);
        expectedSortValFloat.put(225, 34.17549467);
        expectedSortValFloat.put(3, 45.40283);
        actualHashMap = (LinkedHashMap) resMap.get("sortValFloat");
        assertEquals(expectedSortValFloat, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortValFloat.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Object> expectedSortValMixedNumeric = new LinkedHashMap<>();
        expectedSortValMixedNumeric.put(225, "-9223372036854775808");
        expectedSortValMixedNumeric.put(4, 5);
        expectedSortValMixedNumeric.put(2, "8");
        expectedSortValMixedNumeric.put(5, 40);
        expectedSortValMixedNumeric.put(300, 200);
        expectedSortValMixedNumeric.put(3, "700");
        expectedSortValMixedNumeric.put(9, 9223372036854775807L);
        actualHashMap = (LinkedHashMap) resMap.get("sortValMixedNumeric");
        assertEquals(expectedSortValMixedNumeric, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortValMixedNumeric.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);
    }

    public void testExecutionHashMapSortByKeyAsc() {
        String body = "var msg = {};\n" +
                "var sortValString = {\"21Dragon\":2, \"Brain\":3, \"20\":4, \"30\":1};\n" +
                "sortValString.sortByKey();\n" +
                "var sortValInt = {2:1, 5:30, 225:4, 8:-214748, 1:57, 3:214748, 9:100000};\n" +
                "sortValInt.sortByKey();\n" +
                "var sortValIntLong = {2:45, 9223372036854775807:5, 225:30, 8:40, -9223372036854775808:3, 9:1000};\n" +
                "sortValIntLong.sortByKey();\n" +
                "var sortValFloat = {3.40282:2, 34.17549467:225, 45.40283:3, 1.1754943:5};\n" +
                "sortValFloat.sortByKey();\n" +
                "var sortValMixedNumeric = {\"8\":21, \"-9223372036854775808\":225, \"700\":3, 40:40, 9223372036854775807:9, 4:5, 300:200};\n" +
                "sortValMixedNumeric.sortByKey();\n" +
                "\n" +
                "msg.sortValString = sortValString;\n" +
                "msg.sortValInt = sortValInt;\n" +
                "msg.sortValIntLong = sortValIntLong;\n" +
                "msg.sortValFloat = sortValFloat;\n" +
                "msg.sortValMixedNumeric = sortValMixedNumeric;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<Object, Object> expectedSortKeyString = new LinkedHashMap<>();
        expectedSortKeyString.put("20", 4);
        expectedSortKeyString.put("21Dragon", 2);
        expectedSortKeyString.put("30", 1);
        expectedSortKeyString.put("Brain", 3);
        LinkedHashMap actualHashMap = (LinkedHashMap) resMap.get("sortValString");
        assertEquals(expectedSortKeyString, actualHashMap);
        assertEquals(expectedSortKeyString.keySet(), actualHashMap.keySet());
        ArrayList expectedList = new ArrayList<Object>(expectedSortKeyString.keySet());
        ArrayList actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Object, Object> expectedSortKeyInt = new LinkedHashMap<>();
        expectedSortKeyInt.put(1, 57);
        expectedSortKeyInt.put(2, 1);
        expectedSortKeyInt.put(3, 214748);
        expectedSortKeyInt.put(5, 30);
        expectedSortKeyInt.put(8, -214748);
        expectedSortKeyInt.put(9, 100000);
        expectedSortKeyInt.put(225, 4);
        actualHashMap = (LinkedHashMap) resMap.get("sortValInt");
        assertEquals(expectedSortKeyInt, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortKeyInt.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Object, Object> expectedSortKeyIntLong = new LinkedHashMap<>();
        expectedSortKeyIntLong.put(-9223372036854775808L, 3);
        expectedSortKeyIntLong.put(2, 45);
        expectedSortKeyIntLong.put(8, 40);
        expectedSortKeyIntLong.put(9, 1000);
        expectedSortKeyIntLong.put(225, 30);
        expectedSortKeyIntLong.put(9223372036854775807L, 5);
        actualHashMap = (LinkedHashMap) resMap.get("sortValIntLong");
        assertEquals(expectedSortKeyIntLong, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortKeyIntLong.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Double, Object> expectedSortValFloat = new LinkedHashMap<>();
        expectedSortValFloat.put(1.1754943, 5);
        expectedSortValFloat.put(3.40282, 2);
        expectedSortValFloat.put(34.17549467, 225);
        expectedSortValFloat.put(45.40283, 3);
        actualHashMap = (LinkedHashMap) resMap.get("sortValFloat");
        assertEquals(expectedSortValFloat, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortValFloat.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Object, Object> expectedSortValMixedNumeric = new LinkedHashMap<>();
        expectedSortValMixedNumeric.put("-9223372036854775808", 225);
        expectedSortValMixedNumeric.put(4, 5);
        expectedSortValMixedNumeric.put("8", 21);
        expectedSortValMixedNumeric.put(40, 40);
        expectedSortValMixedNumeric.put(300, 200);
        expectedSortValMixedNumeric.put("700", 3);
        expectedSortValMixedNumeric.put(9223372036854775807L, 9);
        actualHashMap = (LinkedHashMap) resMap.get("sortValMixedNumeric");
        assertEquals(expectedSortValMixedNumeric, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortValMixedNumeric.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);
    }

    public void testExecutionHashMapSortByKeyDesc() {
        String body = "var msg = {};\n" +
                "var sortValString = {\"21Dragon\":2, \"Brain\":3, \"20\":4, \"30\":1};\n" +
                "sortValString.sortByKey(false);\n" +
                "var sortValInt = {2:1, 5:30, 225:4, 8:-214748, 1:57, 3:214748, 9:100000};\n" +
                "sortValInt.sortByKey(false);\n" +
                "var sortValIntLong = {2:45, 9223372036854775807:5, 225:30, 8:40, -9223372036854775808:3, 9:1000};\n" +
                "sortValIntLong.sortByKey(false);\n" +
                "var sortValFloat = {3.40282:2, 34.17549467:225, 45.40283:3, 1.1754943:5};\n" +
                "sortValFloat.sortByKey(false);\n" +
                "var sortValMixedNumeric = {\"8\":21, \"-9223372036854775808\":225, \"700\":3, 40:40, 9223372036854775807:9, 4:5, 300:200};\n" +
                "sortValMixedNumeric.sortByKey(false);\n" +
                "\n" +
                "msg.sortValString = sortValString;\n" +
                "msg.sortValInt = sortValInt;\n" +
                "msg.sortValIntLong = sortValIntLong;\n" +
                "msg.sortValFloat = sortValFloat;\n" +
                "msg.sortValMixedNumeric = sortValMixedNumeric;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<Object, Object> expectedSortKeyString = new LinkedHashMap<>();
        expectedSortKeyString.put("20", 4);
        expectedSortKeyString.put("21Dragon", 2);
        expectedSortKeyString.put("30", 1);
        expectedSortKeyString.put("Brain", 3);
        LinkedHashMap actualHashMap = (LinkedHashMap) resMap.get("sortValString");
        assertEquals(expectedSortKeyString, actualHashMap);
        ArrayList expectedList = new ArrayList<Object>(expectedSortKeyString.keySet());
        ArrayList actualList = new ArrayList<Object>(actualHashMap.keySet());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Object, Object> expectedSortKeyInt = new LinkedHashMap<>();
        expectedSortKeyInt.put(1, 57);
        expectedSortKeyInt.put(2, 1);
        expectedSortKeyInt.put(3, 214748);
        expectedSortKeyInt.put(5, 30);
        expectedSortKeyInt.put(8, -214748);
        expectedSortKeyInt.put(9, 100000);
        expectedSortKeyInt.put(225, 4);
        actualHashMap = (LinkedHashMap) resMap.get("sortValInt");
        assertEquals(expectedSortKeyInt, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortKeyInt.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Object, Object> expectedSortKeyIntLong = new LinkedHashMap<>();
        expectedSortKeyIntLong.put(-9223372036854775808L, 3);
        expectedSortKeyIntLong.put(2, 45);
        expectedSortKeyIntLong.put(8, 40);
        expectedSortKeyIntLong.put(9, 1000);
        expectedSortKeyIntLong.put(225, 30);
        expectedSortKeyIntLong.put(9223372036854775807L, 5);
        actualHashMap = (LinkedHashMap) resMap.get("sortValIntLong");
        assertEquals(expectedSortKeyIntLong, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortKeyIntLong.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Double, Object> expectedSortValFloat = new LinkedHashMap<>();
        expectedSortValFloat.put(1.1754943, 5);
        expectedSortValFloat.put(3.40282, 2);
        expectedSortValFloat.put(34.17549467, 225);
        expectedSortValFloat.put(45.40283, 3);
        actualHashMap = (LinkedHashMap) resMap.get("sortValFloat");
        assertEquals(expectedSortValFloat, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortValFloat.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Object, Object> expectedSortValMixedNumeric = new LinkedHashMap<>();
        expectedSortValMixedNumeric.put("-9223372036854775808", 225);
        expectedSortValMixedNumeric.put(4, 5);
        expectedSortValMixedNumeric.put("8", 21);
        expectedSortValMixedNumeric.put(40, 40);
        expectedSortValMixedNumeric.put(300, 200);
        expectedSortValMixedNumeric.put("700", 3);
        expectedSortValMixedNumeric.put(9223372036854775807L, 9);
        actualHashMap = (LinkedHashMap) resMap.get("sortValMixedNumeric");
        assertEquals(expectedSortValMixedNumeric, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortValMixedNumeric.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);
    }

    public void testExecutionArrayListSortNumericWithString_Error() {
        String body = "var msg = {};\n" +
                "var mixedNumericStringArray = [\"8\", \"9\", 'Jabnm', 40, 1, 5, 200];\n" +
                "mixedNumericStringArray.sort();\n" +
                "msg.mixedNumericStringArray = mixedNumericStringArray;\n" +
                "return {msg: msg};";
        try {
            executeScript(body);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("For input string: \"Jabnm\""));
        }
    }

    public void testExecutionHashMapSortByValueNumericWithString_Error() {
        String body = "var msg = {};\n" +
                "var sortValMixedNumeric = {2:\"8\", 225:\"-9223372036854775808\", 3:\"rt700\", 5:40, 9:9223372036854775807, 4:5, 300:200};\n" +
                "sortValMixedNumeric.sortByValue(false);\n" +
                "msg.sortValMixedNumeric = sortValMixedNumeric;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        try {
            executeScript(body);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("For input string: \"rt700\""));
        }
    }

    public void testExecutionHashMapSortByKeyNumericWithString_Error() {
        String body = "var msg = {};\n" +
                "var sortValMixedNumeric = {\"8\":21, \"-9223372036854775808\":225, \"rt700\":3, 40:40, 9223372036854775807:9, 4:5, 300:200};\n" +
                "sortValMixedNumeric.sortByKey();\n" +
                "\n" +
                "\n" +
                "msg.sortValMixedNumeric = sortValMixedNumeric;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        try {
            executeScript(body);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("For input string: \"rt700\""));
        }
    }

    public void testExecutionArrayList_Reverse() {
        String body = "var msg = {};\n" +
                "var array = [\"8\", 40, 9223372036854775807, \"Dec\", \"-9223372036854775808\"];\n" +
                "msg.array = array.slice();\n" +
                "array.reversed();\n" +
                "msg.arrayRev = array;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add("8");
        expectedArray.add(40);
        expectedArray.add(9223372036854775807L);
        expectedArray.add("Dec");
        expectedArray.add("-9223372036854775808");
        List actualArray = (List) resMap.get("array");
        assertEquals(expectedArray, actualArray);
        Collections.reverse(expectedArray);
        actualArray = (List) resMap.get("arrayRev");
        assertEquals(expectedArray, actualArray);
    }

    public void testExecutionArrayList_toReversed() {
        String body = "var msg = {};\n" +
                "var array = [\"8\", 40, 9223372036854775807, \"Dec\", \"-9223372036854775808\"];\n" +
                "msg.array = array;\n" +
                "msg.arrayRev = array.toReversed();\n" +
                "return {msg: msg}";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add("8");
        expectedArray.add(40);
        expectedArray.add(9223372036854775807L);
        expectedArray.add("Dec");
        expectedArray.add("-9223372036854775808");
        List actualArray = (List) resMap.get("array");
        assertEquals(expectedArray, actualArray);
        Collections.reverse(expectedArray);
        actualArray = (List) resMap.get("arrayRev");
        assertEquals(expectedArray, actualArray);
    }

    public void testExecutionArrayList_slice() {
        String body = "var msg = {};\n" +
                "var arrayWithEmpty = [\"8\", 40, 9223372036854775807, , \"-9223372036854775808\"];\n" +
                "var array = [\"8\", 40, 9223372036854775807, \"Dec\", \"-9223372036854775808\"];\n" +
                "msg.array = array;\n" +
                "msg.arrayWithEmpty = arrayWithEmpty;\n" +
                "msg.arraySlice = array.slice();\n" +
                "msg.arraySliceWithEmpty = arrayWithEmpty.slice();\n" +
                "msg.arraySlice2 = array.slice(2);\n" +
                "msg.arraySlice5 = array.slice(5);\n" +
                "msg.arraySlice2_4 = array.slice(2, 4);\n" +
                "msg.arraySlice1_5 = array.slice(1, 5);\n" +
                "msg.arraySliceStr3_4 = array.slice(\"3\", \"4\");\n" +
                "msg.arraySlice_2 = array.slice(-2);\n" +
                "msg.arraySlice2_1 = array.slice(2, -1);\n" +
                "return {msg: msg}";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List actualArray = (List) resMap.get("arrayWithEmpty");
        List expectedArray = (List) resMap.get("arraySliceWithEmpty");
        assertEquals(expectedArray, actualArray);
        List expected = actualArray = (List) resMap.get("array");
        expectedArray = (List) resMap.get("arraySlice");
        assertEquals(expectedArray, actualArray);
        expectedArray = expected.subList(2, expected.size());
        actualArray = (List) resMap.get("arraySlice2");
        assertEquals(expectedArray, actualArray);
        expectedArray = expected.subList(5, expected.size());
        actualArray = (List) resMap.get("arraySlice5");
        assertEquals(expectedArray, actualArray);
        expectedArray = expected.subList(2, 4);
        actualArray = (List) resMap.get("arraySlice2_4");
        assertEquals(expectedArray, actualArray);
        expectedArray = expected.subList(1, 5);
        actualArray = (List) resMap.get("arraySlice1_5");
        assertEquals(expectedArray, actualArray);
        expectedArray = expected.subList(3, 4);
        actualArray = (List) resMap.get("arraySliceStr3_4");
        assertEquals(expectedArray, actualArray);
        expectedArray = expected.subList(-2 + expected.size(), 5);
        actualArray = (List) resMap.get("arraySlice_2");
        assertEquals(expectedArray, actualArray);
        expectedArray = expected.subList(2, -1 + expected.size());
        actualArray = (List) resMap.get("arraySlice2_1");
        assertEquals(expectedArray, actualArray);
    }

    public void testExecutionArrayList_sliceWithStartMoreEnd_Error() {
        String body = "var msg = {};\n" +
                "var array = [\"8\", 40, 9223372036854775807, \"Dec\", \"-9223372036854775808\"];\n" +
                "msg.arraySlice6 = array.slice(6);\n" +
                "return {msg: msg}";
        try {
            executeScript(body);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("fromIndex(6) > toIndex(5)"));
        }
    }

    public void testExecutionArrayList_sliceWithStartNotNumeric_Error() {
        String body = "var msg = {};\n" +
                "var array = [\"8\", 40, 9223372036854775807, \"Dec\", \"-9223372036854775808\"];\n" +
                "msg.arraySlice6 = array.slice(\"rt\", \"2\");\n" +
                "return {msg: msg}";
        try {
            executeScript(body);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("For input string: \"rt\""));
        }
    }

    public void testExecutionArrayList_sliceWithEndNotNumeric_Error() {
        String body = "var msg = {};\n" +
                "var array = [\"8\", 40, 9223372036854775807, \"Dec\", \"-9223372036854775808\"];\n" +
                "msg.arraySlice6 = array.slice(\"1\", \"Dec\");\n" +
                "return {msg: msg}";
        try {
            executeScript(body);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("For input string: \"Dec\""));
        }
    }

    public void testExecutionArrayList_indexOf() {
        String body = "var msg = {};\n" +
                "var arrayInd = [\"a\", 34, \"a\", 34, \"a\", \"a\"];\n" +
                "var msg.rezInd = arrayInd.indexOf(\"a\", 1) ;\n" +
                "return {msg: msg}";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        int actualInd = (int) resMap.get("rezInd");
        assertEquals(2, actualInd);
        body = "var msg = {};\n" +
                "var arrayInd = [\"a\", 34, \"a\", 34, \"a\", \"a\"];\n" +
                "var msg.rezInd = arrayInd.indexOf(\"a\", 3) ;\n" +
                "return {msg: msg}";
        result = executeScript(body);
        resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        actualInd = (int) resMap.get("rezInd");
        assertEquals(4, actualInd);
        body = "var msg = {};\n" +
                "var arrayInd = [\"a\", 34, \"a\", 34, \"a\", \"a\"];\n" +
                "var msg.rezInd = arrayInd.indexOf(\"a\", 5) ;\n" +
                "return {msg: msg}";
        result = executeScript(body);
        resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        actualInd = (int) resMap.get("rezInd");
        assertEquals(5, actualInd);
        body = "var msg = {};\n" +
                "var arrayInd = [\"a\", 34, \"a\", 34, \"a\", \"a\"];\n" +
                "var msg.rezInd = arrayInd.indexOf(\"a\", 5) ;\n" +
                "return {msg: msg}";
        result = executeScript(body);
        resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        actualInd = (int) resMap.get("rezInd");
        assertEquals(5, actualInd);
        body = "var msg = {};\n" +
                "var arrayInd = [\"a\", 34, \"a\", 34, \"a\", \"a\"];\n" +
                "var msg.rezInd = arrayInd.indexOf(34, 2) ;\n" +
                "return {msg: msg}";
        result = executeScript(body);
        resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        actualInd = (int) resMap.get("rezInd");
        assertEquals(3, actualInd);
        body = "var msg = {};\n" +
                "var arrayInd = [\"a\", 34, \"a\", 34, \"a\", \"a\"];\n" +
                "var msg.rezInd = arrayInd.indexOf(34, 4) ;\n" +
                "return {msg: msg}";
        result = executeScript(body);
        resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        actualInd = (int) resMap.get("rezInd");
        assertEquals(-1, actualInd);
    }

    private Object executeScript(String ex, Map vars, ExecutionContext executionContext, long timeoutMs) throws Exception {
        final CountDownLatch countDown = new CountDownLatch(1);
        AtomicReference<Object> result = new AtomicReference<>();
        AtomicReference<Exception> exception = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                result.set(executeScript(ex, vars, executionContext));
            } catch (Exception e) {
                exception.set(e);
            } finally {
                countDown.countDown();
            }
        });
        thread.start();
        try {
            countDown.await(timeoutMs, TimeUnit.MILLISECONDS);
            executionContext.stop();
            countDown.await(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (exception.get() != null) {
            throw exception.get();
        } else {
            return result.get();
        }
    }

    private Object executeScript(String ex) {
        return executeScript(ex, new HashMap());
    }

    private Object executeScript(String ex, Map vars) {
        return executeScript(ex, vars, new ExecutionContext(this.parserConfig));
    }

    private Object executeScript(String ex, Map vars, ExecutionContext executionContext) {
        Serializable compiled = compileExpression(ex, new ParserContext());
        this.currentExecutionContext = executionContext;
        return executeTbExpression(compiled, this.currentExecutionContext, vars);
    }

    public static final class TestUtil {
        public static String getFoo(Map input) {
            if (input.containsKey("foo")) {
                return input.get("foo") != null ? input.get("foo").toString() : "null";
            } else {
                return "Not found!";
            }
        }

        public static Map methodWithExecContext(String key, Object val, ExecutionContext ctx) {
            Map map = new ExecutionHashMap(1, ctx);
            map.put(key, val);
            return map;
        }

        public static Map methodWithExecContext2(String key, ExecutionContext ctx, Object val) {
            Map map = new ExecutionHashMap(1, ctx);
            map.put(key, val);
            return map;
        }

        public static Map methodWithExecContext3(ExecutionContext ctx, String key, Object val) {
            Map map = new ExecutionHashMap(1, ctx);
            map.put(key, val);
            return map;
        }

        public static List methodWithExecContextVarArgs(ExecutionContext ctx, Object... vals) {
            List list = new ExecutionArrayList(Arrays.asList(vals), ctx);
            return list;
        }
    }

    public static final class MyTestClass {
        private final String innerValue;
        public MyTestClass(String val) {
            this.innerValue = val;
        }

        public String getValue() {
            return innerValue;
        }
    }
}
