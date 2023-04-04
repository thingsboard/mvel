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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeTbExpression;

public class TbExpressionsTest extends TestCase {

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
