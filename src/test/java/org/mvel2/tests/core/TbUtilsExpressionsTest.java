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

    private static final int BYTES_LEN_LONG_MAX = 8;

    private static final int HEX_RADIX = 16;

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
                    "[Near : {... var newMsg = stringToBytes(msgTest); ....}]\n" +
                    "                          ^\n" +
                    "[Line: 2, Column: 14]"));
        }
    }

    public void testSHexToBytes_InFunction_Ok() throws Exception {
        String scriptBody = "var data = frm();\n" +
                "function frm(){\n" +
                "    var out = {};\n" +
                "    out.bb = hexToBytes(\"0x01752B0367FA000500010488FFFFFFFFFFFFFFFF33\");\n" +
                "    return out;\n" +
                "}\n" +
                "var result = {\n" +
                "   msg: data\n" +
                "};\n" +
                "return result;";
        LinkedHashMap<String, LinkedHashMap<String, List<Byte>>> expected = new LinkedHashMap<>();
        byte[] expectedBytes = {1, 117, 43, 3, 103, -6, 0, 5, 0, 1, 4, -120, -1, -1, -1, -1, -1, -1, -1, -1, 51};
        List<Byte> expBytesToList = bytesToList(expectedBytes);
        LinkedHashMap<String, List<Byte>> expIntList = new LinkedHashMap<>();
        expIntList.put("bb", expBytesToList);
        expected.put("msg", expIntList);
        Object actual = executeScript(scriptBody);
        assertEquals(expected, actual);
    }

    public void testIntToHex_ArgumentIntMax_Ok() throws Exception {
        String expectedStr = "0x7FFFFFFF";
        String scriptBody = "\n" +
                "var newMsg = intToHex(0x7FFFFFFF, true, true);\n" +
                "return {msg: newMsg}";
        LinkedHashMap<String, String> expected = new LinkedHashMap<>();
        expected.put("msg", expectedStr);
        Object actual = executeScript(scriptBody);
        assertEquals(expected, actual);
    }

    public void testIntToHex_ArgumentIntMoreMax_Ok() throws Exception {
        String expectedStr = "0xFFD8FFA6";
        String scriptBody = "\n" +
                "var newMsg = longToHex(0xFFD8FFA6, true, true);\n" +
                "return {msg: newMsg}";
        LinkedHashMap<String, String> expected = new LinkedHashMap<>();
        expected.put("msg", expectedStr);
        Object actual = executeScript(scriptBody);
        assertEquals(expected, actual);
    }

    public void testIntToHex_ArgumentIntMin_Ok() throws Exception {
        String expectedStr = "0x80000000";
        String scriptBody = "\n" +
                "var newMsg = intToHex(0x80000000, true, true);\n" +
                "return {msg: newMsg}";
        LinkedHashMap<String, String> expected = new LinkedHashMap<>();
        expected.put("msg", expectedStr);
        Object actual = executeScript(scriptBody);
        assertEquals(expected, actual);
    }

    public void testLongToHex_ArgumentLongMax_Ok() throws Exception {
        String expectedStr = "0x7FFFFFFFFFFFFFFF";
        String scriptBody = "\n" +
                "var newMsg = longToHex(0x7FFFFFFFFFFFFFFF, true, true);\n" +
                "return {msg: newMsg}";
        LinkedHashMap<String, String> expected = new LinkedHashMap<>();
        expected.put("msg", expectedStr);
        Object actual = executeScript(scriptBody);
        assertEquals(expected, actual);
    }

    public void testLongToHex_ArgumentLonMin_Ok() throws Exception {
        String expectedStr = "0x8000000000000000";
        String scriptBody = "\n" +
                "var newMsg = longToHex(0x8000000000000000, true, true);\n" +
                "return {msg: newMsg}";
        LinkedHashMap<String, String> expected = new LinkedHashMap<>();
        expected.put("msg", expectedStr);
        Object actual = executeScript(scriptBody);
        assertEquals(expected, actual);
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
        private static final int HEX_LEN_MIN = -1;
        private static final int HEX_LEN_LONG_MAX = 16;
        private static final int BYTES_LEN_LONG_MAX = 8;
        private static final int HEX_LEN_INT_MAX = 8;

        public static void register(ParserConfiguration parserConfig) throws Exception {
            parserConfig.addImport("stringToBytes", new MethodStub(TbUtils.class.getMethod("stringToBytes",
                    ExecutionContext.class, Object.class)));
            parserConfig.addImport("stringToBytes", new MethodStub(TbUtils.class.getMethod("stringToBytes",
                    ExecutionContext.class, Object.class, String.class)));
            parserConfig.registerNonConvertableMethods(TbUtils.class, Collections.singleton("stringToBytes"));
            parserConfig.addImport("hexToBytes", new MethodStub(TbUtils.class.getMethod("hexToBytes",
                    ExecutionContext.class, String.class)));
            parserConfig.addImport("longToHex", new MethodStub(TbUtils.class.getMethod("longToHex",
                    Long.class, boolean.class, boolean.class)));
            parserConfig.addImport("intToHex", new MethodStub(TbUtils.class.getMethod("intToHex",
                    Integer.class, boolean.class, boolean.class)));
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

        public static ExecutionArrayList<Byte> hexToBytes(ExecutionContext ctx, String value) {
            String hex = prepareNumberString(value, true);
            int len = hex.length();
            if (len % 2 > 0) {
                throw new IllegalArgumentException("Hex string must be even-length.");
            }
            ExecutionArrayList<Byte> data = new ExecutionArrayList<>(ctx);
            for (int i = 0; i < hex.length(); i += 2) {
                // Extract two characters from the hex string
                String byteString = hex.substring(i, i + 2);
                // Parse the hex string to a byte
                byte byteValue = (byte) Integer.parseInt(byteString, HEX_RADIX);
                // Add the byte to the ArrayList
                data.add(byteValue);
            }
            return data;
        }

        private static List<Byte> bytesToList(ExecutionContext ctx, byte[] bytes) {
            List<Byte> list = new ExecutionArrayList<>(ctx);
            for (byte aByte : bytes) {
                list.add(aByte);
            }
            return list;
        }

        private static String prepareNumberString(String value, boolean bigEndian) {
            if (isNotBlank(value)) {
                value = value.trim();
                value = value.replace("0x", "");
                value = value.replace("0X", "");
                value = value.replace(",", ".");
                return bigEndian ? value : reverseHexStringByOrder(value);
            }
            return null;
        }

        private static boolean isNotBlank(String source) {
            return source != null && !source.isEmpty() && !source.trim().isEmpty();
        }

        private static String reverseHexStringByOrder(String value) {
            if (value.startsWith("-")) {
                throw new IllegalArgumentException("The hexadecimal string must be without a negative sign.");
            }
            boolean isHexPref = value.startsWith("0x");
            String hex = isHexPref ? value.substring(2) : value;
            if (hex.length() % 2 > 0) {
                throw new IllegalArgumentException("The hexadecimal string must be even-length.");
            }
            // Split the hex string into bytes (2 characters each)
            StringBuilder reversedHex = new StringBuilder(BYTES_LEN_LONG_MAX);
            for (int i = hex.length() - 2; i >= 0; i -= 2) {
                reversedHex.append(hex, i, i + 2);
            }
            String result = reversedHex.toString();
            return isHexPref ? "0x" + result : result;
        }

        public static String intToHex(Integer i, boolean bigEndian, boolean pref) {
            return prepareNumberHexString(i.longValue(), bigEndian, pref, HEX_LEN_MIN, HEX_LEN_INT_MAX);
        }

        public static String longToHex(Long l, boolean bigEndian, boolean pref) {
            return prepareNumberHexString(l, bigEndian, pref, HEX_LEN_MIN, HEX_LEN_LONG_MAX);
        }

        private static String prepareNumberHexString(Long number, boolean bigEndian, boolean pref, int len, int hexLenMax) {
            String hex = Long.toHexString(number).toUpperCase();
            hexLenMax = hexLenMax < 0 ? hex.length() : hexLenMax;
            String hexWithoutZeroFF = removeLeadingZero_FF(hex, number, hexLenMax);
            hexWithoutZeroFF = bigEndian ? hexWithoutZeroFF : reverseHexStringByOrder(hexWithoutZeroFF);
            len = len == HEX_LEN_MIN ? hexWithoutZeroFF.length() : len;
            String result = hexWithoutZeroFF.substring(hexWithoutZeroFF.length() - len);
            return pref ? "0x" + result : result;
        }

        private static String removeLeadingZero_FF(String hex, Long number, int hexLenMax) {
            String hexWithoutZero = hex.replaceFirst("^0+(?!$)", ""); // Remove leading zeros except for the last one
            hexWithoutZero = hexWithoutZero.length() % 2 > 0 ? "0" + hexWithoutZero : hexWithoutZero;
            if (number >= 0) {
                return hexWithoutZero;
            } else {
                String hexWithoutZeroFF = hexWithoutZero.replaceFirst("^F+(?!$)", "");
                hexWithoutZeroFF = hexWithoutZeroFF.length() % 2 > 0 ? "F" + hexWithoutZeroFF : hexWithoutZeroFF;
                if (hexWithoutZeroFF.length() > hexLenMax) {
                    return hexWithoutZeroFF.substring(hexWithoutZeroFF.length() - hexLenMax);
                } else if (hexWithoutZeroFF.length() == hexLenMax) {
                    return hexWithoutZeroFF;
                } else {
                    return "FF" + hexWithoutZeroFF;
                }
            }
        }
    }
}
