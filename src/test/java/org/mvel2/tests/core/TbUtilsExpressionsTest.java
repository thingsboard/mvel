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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeTbExpression;

public class TbUtilsExpressionsTest extends TestCase {

    private ExecutionContext ctx;
    SandboxedParserConfiguration parserConfig;
    private ExecutionContext currentExecutionContext;

    @Override
    protected void setUp() throws Exception {
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
        super.setUp();
        this.parserConfig = ParserContext.enableSandboxedMode();
        try {
            TbUtils.register(parserConfig);
            parserConfig.addImport(TbUtilsExpressionsTest.TbUtils.class);
            parserConfig.addNonConvertableClass(TbUtils.class.getName());
        } catch (Exception e) {
            System.out.println("Cannot register functions " +e.getMessage());
        }
        ctx = new ExecutionContext(parserConfig);
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

    /**
     * The first: ReflectiveAccessorOptimizer
     * var msgTest = {
     *     "hello": "world"
     * };
     * org.mvel2.execution.ExecutionHashMap
     *    public String toString() {
     *         String res = super.toString();
     *         return "(id=" + id + ") " + res;
     *     }
     * return String actualStr = "(id=1) {hello=world}"
     * Another: MVELRuntime -> MethodAccessor
     * ! Not have Class[] argParameterTypes = removeExecutionContextParam(parameterTypes); !
     * @throws Exception
     */
    public void testStringToBytes_ArgumentTypeIsNotString_Bad() throws Exception {
        String argument = "msgTest";
        String scriptBody = "var " + argument + "  = {\"hello\": \"world\"}; \n" +
                "var newMsg = stringToBytes(" + argument + ");\n" +
                "\n" +
                "return {msg: newMsg};";
        try {
            executeScript(scriptBody);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("Invalid type of the '" + argument + "' parameter. Expected 'String' but was 'Map'"));
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
                    ExecutionContext.class, String.class)));
            parserConfig.addImport("stringToBytes", new MethodStub(TbUtils.class.getMethod("stringToBytes",
                    ExecutionContext.class, String.class, String.class)));
        }

        public static List<Byte> stringToBytes(ExecutionContext ctx, String str) {
            byte[] bytes = str.getBytes();
            return bytesToList(ctx, bytes);
        }

        public static List<Byte> stringToBytes(ExecutionContext ctx, String str, String charsetName) throws UnsupportedEncodingException {
            byte[] bytes = str.getBytes(charsetName);
            return bytesToList(ctx, bytes);
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
