package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.CompileException;
import org.mvel2.ExecutionContext;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.SandboxedParserConfiguration;
import org.mvel2.execution.ExecutionArrayList;
import org.mvel2.optimizers.OptimizerFactory;
import org.mvel2.util.MethodStub;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeTbExpression;

public class TbUtilsExpressionsTest extends TestCase {

    private SandboxedParserConfiguration parserConfig;

    @Override
    protected void setUp() throws Exception {
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
        super.setUp();
        this.parserConfig = ParserContext.enableSandboxedMode();
        try {
            TbUtils.register(parserConfig);
            parserConfig.addImport(TbUtilsExpressionsTest.TbUtils.class);
        } catch (Exception e) {
            System.out.println("Cannot register functions " +e.getMessage());
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        ParserContext.disableSandboxedMode();
    }

    public void testStringToBytes_ArgumentTypeIsString_Ok() throws Exception {
        String expectedStr = "{\"hello\": \"world\"}";
        String scriptBody = "var input = \"{\\\"hello\\\": \\\"world\\\"}\"; \n" +
                "var newMsg = stringToBytes(input);\n" +
                "\n" +
                "return {msg: newMsg};";
        LinkedHashMap<String, List<Byte>> expected = new LinkedHashMap<>();
        List<Byte> expIntList = bytesToList(expectedStr.getBytes());
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBody);
        assertEquals(expected, actual);
    }

    public void testStringToBytes_ArgumentTypeIsObjectAsString_Ok() throws Exception {
        String expectedStr = "world";
        String scriptBody = "var dataMap = {};\n" +
                "dataMap.hello = \"world\";\n" +
                "var newMsg =  stringToBytes(dataMap.get(\"hello\"));\n" +
                "return {msg: newMsg}";
        LinkedHashMap<String, List<Byte>> expected = new LinkedHashMap<>();
        List<Byte> expIntList = bytesToList(expectedStr.getBytes());
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBody);
        assertEquals(expected, actual);
    }

    public void testStringToBytes_ArgumentTypeIsNotStringLineNotZero_Bad() throws Exception {
        String argument = "msgTest";
        String scriptBody = "var " + argument + "  = {\"hello\": \"world\"}; \n" +
                "var newMsg = stringToBytes(" + argument + ");\n" +
                "\n" +
                "return {msg: newMsg};";
        try {
            executeScript(scriptBody);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().equals("[Error: stringToBytes(msgTest): Invalid type parameter [ExecutionHashMap]. Expected 'String']\n" +
                    "[Near : {... wMsg = stringToBytes(msgTest); ....}]\n" +
                    "                                 ^\n" +
                    "[Line: 2, Column: 27]"));
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
        return executeTbExpression(compiled, executionContext, vars);
    }

    private List<Byte> bytesToList(byte[] bytes) {
        List<Byte> list = new ArrayList<>();
        for (byte aByte : bytes) {
            list.add(aByte);
        }
        return list;
    }

    public static class TbUtils {

        private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

        public static void register(ParserConfiguration parserConfig) throws Exception {
            parserConfig.addImport("stringToBytes", new MethodStub(TbUtils.class.getMethod("stringToBytes",
                    ExecutionContext.class, Object.class)));
            parserConfig.addImport("stringToBytes", new MethodStub(TbUtils.class.getMethod("stringToBytes",
                    ExecutionContext.class, Object.class, String.class)));
            parserConfig.registerNonConvertableMethods(TbUtils.class, Collections.singleton("stringToBytes"));
        }

        public static List<Byte> stringToBytes(ExecutionContext ctx, Object str) throws IllegalAccessException {
            if (str instanceof String) {
                byte[] bytes = str.toString().getBytes();
                return bytesToList(ctx, bytes);
            } else {
                throw new IllegalAccessException("Invalid type parameter [" + str.getClass().getSimpleName() + "]. Expected 'String'");
            }
        }

        public static List<Byte> stringToBytes(ExecutionContext ctx, Object str, String charsetName) throws UnsupportedEncodingException, IllegalAccessException {
            if (str instanceof String) {
                byte[] bytes = str.toString().getBytes(charsetName);
                return bytesToList(ctx, bytes);
            } else {
                throw new IllegalAccessException("Invalid type parameter [" + str.getClass().getSimpleName() + "]. Expected 'String'");
            }
        }

        private static List<Byte> bytesToList(ExecutionContext ctx, byte[] bytes) {
            List<Byte> list = new ExecutionArrayList<>(ctx);
            for (byte aByte : bytes) {
                list.add(aByte);
            }
            return list;
        }

    }
}
