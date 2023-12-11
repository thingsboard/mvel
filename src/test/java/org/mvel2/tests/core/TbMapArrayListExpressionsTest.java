package org.mvel2.tests.core;

import junit.framework.TestCase;
import org.mvel2.CompileException;
import org.mvel2.ExecutionContext;
import org.mvel2.ParserContext;
import org.mvel2.SandboxedParserConfiguration;
import org.mvel2.optimizers.OptimizerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeTbExpression;

public class TbMapArrayListExpressionsTest extends TestCase {

    private static final Comparator stringCompAsc = new Comparator() {
        public int compare(Object o1, Object o2) {
            String first = String.valueOf(o1);
            String second = String.valueOf(o2);
            return first.compareTo(second);
        }
    };
    private static final Comparator stringCompDesc = new Comparator() {
        public int compare(Object o1, Object o2) {
            String first = String.valueOf(o1);
            String second = String.valueOf(o2);
            return second.compareTo(first);
        }
    };
    private final List<String> expectedArrayString = Arrays.asList("Dec", "Feb", "Jan", "March");
    private final List expectedArrayInteger = Arrays.asList(-214748, 1, 4, 30, 57, 100000, 214748);
    private final List expectedArrayIntLong = Arrays.asList(-9223372036854775808L, 30L, 40L, 45L, 1000L, 9223372036854775807L);
    private final List expectedArrayFloat = Arrays.asList(1.1754943f, 3.40282f, 34.175495f, 45.40283f);

    private final List expectedArrayDouble = Arrays.asList(-922337203685.1754943d, 45.40283d, 1754.40282d, 9223372036854775807.17549467d);
    private final List expectedArrayMixedNumeric = Arrays.asList(1, 5, "8", "9", 40, 200, "700");
    private final List expectedArrayMixedNumericString = Arrays.asList(1, 200, 40, 5, "8", "9", "Aabnm", "Babnm", "Zxc");

    private SandboxedParserConfiguration parserConfig;

    private ExecutionContext currentExecutionContext;

    @Override
    protected void setUp() throws Exception {
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
        super.setUp();
        this.parserConfig = ParserContext.enableSandboxedMode();
    }

    // map

    public void testExecutionHashMapCreate() {
        Object res = executeScript("m = {a: 1}; m.a");
        assertTrue(res instanceof Integer);
        assertEquals(1, res);
    }


    public void testExecutionHashMapCreateEmptyMapAndAssignField() {
        Object res = executeScript("m = {}; m.test = 1; m");
        assertTrue(res instanceof Map);
        assertEquals(1, ((Map) res).size());
        assertEquals(1, ((Map) res).get("test"));
    }


    public void testExecutionHashMapNonExistentField() {
        Object res = executeScript("m = {}; t = m.test; t");
        assertNull(res);
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
        assertEquals(2, ((List) result).size());
        assertEquals("hello", ((List) result).get(0));
        assertEquals("testmap", ((List) result).get(1));
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


    public void testExecutionHashMapSortByValueAsc() {
        String body = "var msg = {};\n" +
                "var sortValString = {4:\"March\", 2:\"Feb\", 3:\"Jan\", 1:\"Dec\"};\n" +
                "sortValString.sortByValue();\n" +
                "var sortValInt = {2: 1, 4: 30, 3: 4, 1: -214748, 5: 57, 7: 214748, 6: 100000};\n" +
                "sortValInt.sortByValue();\n" +
                "var sortValIntLong = {4:45L, 6:9223372036854775807L, 2:30L, 3:40L,  1:-9223372036854775808L, 5:1000L};\n" +
                "sortValIntLong.sortByValue();\n" +
                "var sortValFloat = {2:3.40282f, 3:34.175495f, 4:45.40283f, 1:1.1754943f};\n" +
                "sortValFloat.sortByValue();\n" +
                "var sortValDouble = {3:1754.40282d, 4:9.223372036854776E18d, 2:45.40283d, 1:-9.223372036851755E11d};\n" +
                "sortValDouble.sortByValue();\n" +
                "var sortValMixedNumeric = {3:\"8\", 4:\"9\", 7:\"700\", 1:1, 5:40, 2:5, 6:200};\n" +
                "sortValMixedNumeric.sortByValue();\n" +
                "var sortValMixedNumericString = {5:\"8\", 9:\"Zxc\", 6:\"9\", 7:\"Aabnm\", 1:1, 3:40, 8:\"Babnm\", 4:5, 2:200};\n" +
                "sortValMixedNumericString.sortByValue();\n" +
                "msg.sortValString = sortValString;\n" +
                "msg.sortValInt = sortValInt;\n" +
                "msg.sortValIntLong = sortValIntLong;\n" +
                "msg.sortValFloat = sortValFloat;\n" +
                "msg.sortValDouble = sortValDouble;\n" +
                "msg.sortValMixedNumeric = sortValMixedNumeric;\n" +
                "msg.sortValMixedNumericString = sortValMixedNumericString;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<Integer, String> expectedSortValString = new LinkedHashMap<>();
        expectedSortValString.put(1, expectedArrayString.get(0));
        expectedSortValString.put(2, expectedArrayString.get(1));
        expectedSortValString.put(3, expectedArrayString.get(2));
        expectedSortValString.put(4, expectedArrayString.get(3));
        LinkedHashMap actualHashMap = (LinkedHashMap) resMap.get("sortValString");
        assertEquals(expectedSortValString, actualHashMap);
        ArrayList expectedList = new ArrayList<Object>(expectedSortValString.values());
        ArrayList actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Integer> expectedSortValInt = new LinkedHashMap<>();
        expectedSortValInt.put(1, (Integer) expectedArrayInteger.get(0));
        expectedSortValInt.put(2, (Integer) expectedArrayInteger.get(1));
        expectedSortValInt.put(3, (Integer) expectedArrayInteger.get(2));
        expectedSortValInt.put(4, (Integer) expectedArrayInteger.get(3));
        expectedSortValInt.put(5, (Integer) expectedArrayInteger.get(4));
        expectedSortValInt.put(6, (Integer) expectedArrayInteger.get(5));
        expectedSortValInt.put(7, (Integer) expectedArrayInteger.get(6));
        actualHashMap = (LinkedHashMap) resMap.get("sortValInt");
        assertEquals(expectedSortValInt, actualHashMap);
        expectedList = new ArrayList(expectedSortValInt.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Long> expectedSortValIntLong = new LinkedHashMap<>();
        expectedSortValIntLong.put(1, (Long) expectedArrayIntLong.get(0));
        expectedSortValIntLong.put(2, (Long) expectedArrayIntLong.get(1));
        expectedSortValIntLong.put(3, (Long) expectedArrayIntLong.get(2));
        expectedSortValIntLong.put(4, (Long) expectedArrayIntLong.get(3));
        expectedSortValIntLong.put(5, (Long) expectedArrayIntLong.get(4));
        expectedSortValIntLong.put(6, (Long) expectedArrayIntLong.get(5));
        actualHashMap = (LinkedHashMap) resMap.get("sortValIntLong");
        assertEquals(expectedSortValIntLong, actualHashMap);
        expectedList = new ArrayList(expectedSortValIntLong.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Float> expectedSortValFloat = new LinkedHashMap<>();
        expectedSortValFloat.put(1, (Float) expectedArrayFloat.get(0));
        expectedSortValFloat.put(2, (Float) expectedArrayFloat.get(1));
        expectedSortValFloat.put(3, (Float) expectedArrayFloat.get(2));
        expectedSortValFloat.put(4, (Float) expectedArrayFloat.get(3));
        actualHashMap = (LinkedHashMap) resMap.get("sortValFloat");
        assertEquals(expectedSortValFloat, actualHashMap);
        expectedList = new ArrayList(expectedSortValFloat.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Double> expectedSortValDouble = new LinkedHashMap<>();
        expectedSortValDouble.put(1, (Double) expectedArrayDouble.get(0));
        expectedSortValDouble.put(2, (Double) expectedArrayDouble.get(1));
        expectedSortValDouble.put(3, (Double) expectedArrayDouble.get(2));
        expectedSortValDouble.put(4, (Double) expectedArrayDouble.get(3));
        actualHashMap = (LinkedHashMap) resMap.get("sortValDouble");
        assertEquals(expectedSortValDouble, actualHashMap);
        expectedList = new ArrayList(expectedSortValDouble.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Object> expectedSortValMixedNumeric = new LinkedHashMap<>();
        expectedSortValMixedNumeric.put(1, expectedArrayMixedNumeric.get(0));
        expectedSortValMixedNumeric.put(2, expectedArrayMixedNumeric.get(1));
        expectedSortValMixedNumeric.put(3, expectedArrayMixedNumeric.get(2));
        expectedSortValMixedNumeric.put(4, expectedArrayMixedNumeric.get(3));
        expectedSortValMixedNumeric.put(5, expectedArrayMixedNumeric.get(4));
        expectedSortValMixedNumeric.put(6, expectedArrayMixedNumeric.get(5));
        expectedSortValMixedNumeric.put(7, expectedArrayMixedNumeric.get(6));
        actualHashMap = (LinkedHashMap) resMap.get("sortValMixedNumeric");
        assertEquals(expectedSortValMixedNumeric, actualHashMap);
        expectedList = new ArrayList(expectedSortValMixedNumeric.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Object> expectedSortValMixedNumericString = new LinkedHashMap<>();
        expectedSortValMixedNumericString.put(1, expectedArrayMixedNumericString.get(0));
        expectedSortValMixedNumericString.put(2, expectedArrayMixedNumericString.get(1));
        expectedSortValMixedNumericString.put(3, expectedArrayMixedNumericString.get(2));
        expectedSortValMixedNumericString.put(4, expectedArrayMixedNumericString.get(3));
        expectedSortValMixedNumericString.put(5, expectedArrayMixedNumericString.get(4));
        expectedSortValMixedNumericString.put(6, expectedArrayMixedNumericString.get(5));
        expectedSortValMixedNumericString.put(7, expectedArrayMixedNumericString.get(6));
        expectedSortValMixedNumericString.put(8, expectedArrayMixedNumericString.get(7));
        expectedSortValMixedNumericString.put(9, expectedArrayMixedNumericString.get(8));
        actualHashMap = (LinkedHashMap) resMap.get("sortValMixedNumericString");
        assertEquals(expectedSortValMixedNumericString, actualHashMap);
        expectedList = new ArrayList(expectedSortValMixedNumericString.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);
    }

    public void testExecutionHashMapSortByValueDesc() {
        String body = "var msg = {};\n" +
                "var sortValString = {4:\"March\", 2:\"Feb\", 3:\"Jan\", 1:\"Dec\"};\n" +
                "sortValString.sortByValue(false);\n" +
                "var sortValInt = {2: 1, 4: 30, 3: 4, 1: -214748, 5: 57, 7: 214748, 6: 100000};\n" +
                "sortValInt.sortByValue(false);\n" +
                "var sortValIntLong = {4:45L, 6:9223372036854775807L, 2:30L, 3:40L,  1:-9223372036854775808L, 5:1000L};\n" +
                "sortValIntLong.sortByValue(false);\n" +
                "var sortValFloat = {2:3.40282f, 3:34.175495f, 4:45.40283f, 1:1.1754943f};\n" +
                "sortValFloat.sortByValue(false);\n" +
                "var sortValDouble = {3:1754.40282d, 4:9.223372036854776E18d, 2:45.40283d, 1:-9.223372036851755E11d};\n" +
                "sortValDouble.sortByValue(false);\n" +
                "var sortValMixedNumeric = {3:\"8\", 4:\"9\", 7:\"700\", 1:1, 5:40, 2:5, 6:200};\n" +
                "sortValMixedNumeric.sortByValue(false);\n" +
                "var sortValMixedNumericString = {5:\"8\", 6:\"9\", 7:\"Aabnm\", 1:1, 3:40, 4:5, 2:200};\n" +
                "sortValMixedNumericString.sortByValue(false);\n" +
                "msg.sortValString = sortValString;\n" +
                "msg.sortValInt = sortValInt;\n" +
                "msg.sortValIntLong = sortValIntLong;\n" +
                "msg.sortValFloat = sortValFloat;\n" +
                "msg.sortValDouble = sortValDouble;\n" +
                "msg.sortValMixedNumeric = sortValMixedNumeric;\n" +
                "msg.sortValMixedNumericString = sortValMixedNumericString;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<Integer, String> expectedSortValString = new LinkedHashMap<>();
        expectedSortValString.put(1, expectedArrayString.get(0));
        expectedSortValString.put(2, expectedArrayString.get(1));
        expectedSortValString.put(3, expectedArrayString.get(2));
        expectedSortValString.put(4, expectedArrayString.get(3));
        LinkedHashMap actualHashMap = (LinkedHashMap) resMap.get("sortValString");
        assertEquals(expectedSortValString, actualHashMap);
        ArrayList expectedList = new ArrayList<Object>(expectedSortValString.values());
        ArrayList actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Object> expectedSortValInt = new LinkedHashMap<>();
        expectedSortValInt.put(1, expectedArrayInteger.get(0));
        expectedSortValInt.put(2, expectedArrayInteger.get(1));
        expectedSortValInt.put(3, expectedArrayInteger.get(2));
        expectedSortValInt.put(4, expectedArrayInteger.get(3));
        expectedSortValInt.put(5, expectedArrayInteger.get(4));
        expectedSortValInt.put(6, expectedArrayInteger.get(5));
        expectedSortValInt.put(7, expectedArrayInteger.get(6));
        actualHashMap = (LinkedHashMap) resMap.get("sortValInt");
        assertEquals(expectedSortValInt, actualHashMap);
        expectedList = new ArrayList(expectedSortValInt.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Long> expectedSortValIntLong = new LinkedHashMap<>();
        expectedSortValIntLong.put(1, (Long) expectedArrayIntLong.get(0));
        expectedSortValIntLong.put(2, (Long) expectedArrayIntLong.get(1));
        expectedSortValIntLong.put(3, (Long) expectedArrayIntLong.get(2));
        expectedSortValIntLong.put(4, (Long) expectedArrayIntLong.get(3));
        expectedSortValIntLong.put(5, (Long) expectedArrayIntLong.get(4));
        expectedSortValIntLong.put(6, (Long) expectedArrayIntLong.get(5));
        actualHashMap = (LinkedHashMap) resMap.get("sortValIntLong");
        assertEquals(expectedSortValIntLong, actualHashMap);
        expectedList = new ArrayList(expectedSortValIntLong.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Float> expectedSortValFloat = new LinkedHashMap<>();
        expectedSortValFloat.put(1, (Float) expectedArrayFloat.get(0));
        expectedSortValFloat.put(2, (Float) expectedArrayFloat.get(1));
        expectedSortValFloat.put(3, (Float) expectedArrayFloat.get(2));
        expectedSortValFloat.put(4, (Float) expectedArrayFloat.get(3));
        actualHashMap = (LinkedHashMap) resMap.get("sortValFloat");
        assertEquals(expectedSortValFloat, actualHashMap);
        expectedList = new ArrayList(expectedSortValFloat.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Double> expectedSortValDouble = new LinkedHashMap<>();
        expectedSortValDouble.put(1, (Double) expectedArrayDouble.get(0));
        expectedSortValDouble.put(2, (Double) expectedArrayDouble.get(1));
        expectedSortValDouble.put(3, (Double) expectedArrayDouble.get(2));
        expectedSortValDouble.put(4, (Double) expectedArrayDouble.get(3));
        actualHashMap = (LinkedHashMap) resMap.get("sortValDouble");
        assertEquals(expectedSortValDouble, actualHashMap);
        expectedList = new ArrayList(expectedSortValDouble.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Object> expectedSortValMixedNumeric = new LinkedHashMap<>();
        expectedSortValMixedNumeric.put(1, expectedArrayMixedNumeric.get(0));
        expectedSortValMixedNumeric.put(2, expectedArrayMixedNumeric.get(1));
        expectedSortValMixedNumeric.put(3, expectedArrayMixedNumeric.get(2));
        expectedSortValMixedNumeric.put(4, expectedArrayMixedNumeric.get(3));
        expectedSortValMixedNumeric.put(5, expectedArrayMixedNumeric.get(4));
        expectedSortValMixedNumeric.put(6, expectedArrayMixedNumeric.get(5));
        expectedSortValMixedNumeric.put(7, expectedArrayMixedNumeric.get(6));
        actualHashMap = (LinkedHashMap) resMap.get("sortValMixedNumeric");
        assertEquals(expectedSortValMixedNumeric, actualHashMap);
        expectedList = new ArrayList(expectedSortValMixedNumeric.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Object> expectedSortValMixedNumericString = new LinkedHashMap<>();
        expectedSortValMixedNumericString.put(1, expectedArrayMixedNumericString.get(0));
        expectedSortValMixedNumericString.put(2, expectedArrayMixedNumericString.get(1));
        expectedSortValMixedNumericString.put(3, expectedArrayMixedNumericString.get(2));
        expectedSortValMixedNumericString.put(4, expectedArrayMixedNumericString.get(3));
        expectedSortValMixedNumericString.put(5, expectedArrayMixedNumericString.get(4));
        expectedSortValMixedNumericString.put(6, expectedArrayMixedNumericString.get(5));
        expectedSortValMixedNumericString.put(7, expectedArrayMixedNumericString.get(6));
        actualHashMap = (LinkedHashMap) resMap.get("sortValMixedNumericString");
        assertEquals(expectedSortValMixedNumericString, actualHashMap);
        expectedList = new ArrayList(expectedSortValMixedNumericString.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);
    }

    public void testExecutionHashMapToSortedByValueAsc() {
        String body = "var msg = {};\n" +
                "var sortValString = {4:\"March\", 2:\"Feb\", 3:\"Jan\", 1:\"Dec\"};\n" +
                "msg.toSortedValString = sortValString.toSortedByValue();\n" +
                "msg.sortValString = sortValString;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<Integer, String> expectedSortedValString = new LinkedHashMap<>();
        expectedSortedValString.put(4, expectedArrayString.get(3));
        expectedSortedValString.put(2, expectedArrayString.get(1));
        expectedSortedValString.put(3, expectedArrayString.get(2));
        expectedSortedValString.put(1, expectedArrayString.get(0));

        LinkedHashMap actualHashMap = (LinkedHashMap) resMap.get("sortValString");
        assertEquals(expectedSortedValString, actualHashMap);
        ArrayList expectedList = new ArrayList<Object>(expectedSortedValString.values());
        ArrayList actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, String> expectedToSortedValString = new LinkedHashMap<>();
        expectedToSortedValString.put(1, expectedArrayString.get(0));
        expectedToSortedValString.put(2, expectedArrayString.get(1));
        expectedToSortedValString.put(3, expectedArrayString.get(2));
        expectedToSortedValString.put(4, expectedArrayString.get(3));
        actualHashMap = (LinkedHashMap) resMap.get("toSortedValString");
        assertEquals(expectedToSortedValString, actualHashMap);
        expectedList = new ArrayList<Object>(expectedToSortedValString.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);
    }

    public void testExecutionHashMapToSortedByValueDesc() {
        String body = "var msg = {};\n" +
                "var sortValString = {4:\"March\", 2:\"Feb\", 3:\"Jan\", 1:\"Dec\"};\n" +
                "msg.toSortedValString = sortValString.toSortedByValue(false);\n" +
                "msg.sortValString = sortValString;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<Integer, String> expectedSortedValString = new LinkedHashMap<>();
        expectedSortedValString.put(4, expectedArrayString.get(3));
        expectedSortedValString.put(2, expectedArrayString.get(1));
        expectedSortedValString.put(3, expectedArrayString.get(2));
        expectedSortedValString.put(1, expectedArrayString.get(0));

        LinkedHashMap actualHashMap = (LinkedHashMap) resMap.get("sortValString");
        assertEquals(expectedSortedValString, actualHashMap);
        ArrayList expectedList = new ArrayList<Object>(expectedSortedValString.values());
        ArrayList actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, String> expectedToSortedValString = new LinkedHashMap<>();
        expectedToSortedValString.put(4, expectedArrayString.get(3));
        expectedToSortedValString.put(3, expectedArrayString.get(2));
        expectedToSortedValString.put(2, expectedArrayString.get(1));
        expectedToSortedValString.put(1, expectedArrayString.get(0));
        actualHashMap = (LinkedHashMap) resMap.get("toSortedValString");
        assertEquals(expectedToSortedValString, actualHashMap);
        expectedList = new ArrayList<Object>(expectedToSortedValString.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);
    }

    public void testExecutionHashMapSortByKeyAsc() {
        String body = "var msg = {};\n" +
                "var sortKeyString = {\"Feb\":2, \"March\":4, \"Dec\":1, \"Jan\":3, \"March\":4};\n" +
                "sortKeyString.sortByKey();\n" +
                "var sortKeyInt = {1:2, 57:5, 30:4, 214748:7, -214748:1, 4:3, 100000:6};\n" +
                "sortKeyInt.sortByKey();\n" +
                "var sortKeyIntLong = {30L:2, 1000L:5, 40L:3, -9223372036854775808L:1, 45L:4};\n" +
                "sortKeyIntLong.sortByKey();\n" +
                "var sortKeyFloat = {3.40282f:2, 34.175495f:3, 45.40283f:4, 1.1754943f:1};\n" +
                "sortKeyFloat.sortByKey();\n" +
                "var sortKeyDouble = {45.40283d:2, 9223372036854775807.17549467d:4, -922337203685.1754943d:1,1754.40282d:3};\n" +
                "sortKeyDouble.sortByKey();\n" +
                "var sortKeyMixedNumeric = {\"9\":4,5:2, \"8\":3, \"700\":7, 1:1, 40:5, 200:6};\n" +
                "sortKeyMixedNumeric.sortByKey();\n" +
                "var sortKeyMixedNumericString = {\"Babnm\":8, 200:2, 40:3, \"Zxc\":9, 5:4, \"8\":5, 1:1, \"9\":6, \"Aabnm\":7};\n" +
                "sortKeyMixedNumericString.sortByKey();\n" +
                "msg.sortKeyString = sortKeyString;\n" +
                "msg.sortKeyInt = sortKeyInt;\n" +
                "msg.sortKeyIntLong = sortKeyIntLong;\n" +
                "msg.sortKeyFloat = sortKeyFloat;\n" +
                "msg.sortKeyDouble = sortKeyDouble;\n" +
                "msg.sortKeyMixedNumeric = sortKeyMixedNumeric;\n" +
                "msg.sortKeyMixedNumericString = sortKeyMixedNumericString;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<String, Object> expectedSortKeyString = new LinkedHashMap<>();
        expectedSortKeyString.put(expectedArrayString.get(0), 1);
        expectedSortKeyString.put(expectedArrayString.get(1), 2);
        expectedSortKeyString.put(expectedArrayString.get(2), 3);
        expectedSortKeyString.put(expectedArrayString.get(3), 4);
        LinkedHashMap actualHashMap = (LinkedHashMap) resMap.get("sortKeyString");
        assertEquals(expectedSortKeyString, actualHashMap);
        assertEquals(expectedSortKeyString.keySet(), actualHashMap.keySet());
        ArrayList expectedList = new ArrayList<Object>(expectedSortKeyString.keySet());
        ArrayList actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Object> expectedSortKeyInt = new LinkedHashMap<>();
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(0), 1);
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(1), 2);
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(2), 3);
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(3), 4);
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(4), 5);
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(5), 6);
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(6), 7);
        actualHashMap = (LinkedHashMap) resMap.get("sortKeyInt");
        assertEquals(expectedSortKeyInt, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortKeyInt.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Long, Object> expectedSortKeyIntLong = new LinkedHashMap<>();
        expectedSortKeyIntLong.put((Long) expectedArrayIntLong.get(0), 1);
        expectedSortKeyIntLong.put((Long) expectedArrayIntLong.get(1), 2);
        expectedSortKeyIntLong.put((Long) expectedArrayIntLong.get(2), 3);
        expectedSortKeyIntLong.put((Long) expectedArrayIntLong.get(3), 4);
        expectedSortKeyIntLong.put((Long) expectedArrayIntLong.get(4), 5);
        actualHashMap = (LinkedHashMap) resMap.get("sortKeyIntLong");
        assertEquals(expectedSortKeyIntLong, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortKeyIntLong.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Float, Object> expectedSortKeyFloat = new LinkedHashMap<>();
        expectedSortKeyFloat.put((Float) expectedArrayFloat.get(0), 1);
        expectedSortKeyFloat.put((Float) expectedArrayFloat.get(1), 2);
        expectedSortKeyFloat.put((Float) expectedArrayFloat.get(2), 3);
        expectedSortKeyFloat.put((Float) expectedArrayFloat.get(3), 4);
        actualHashMap = (LinkedHashMap) resMap.get("sortKeyFloat");
        assertEquals(expectedSortKeyFloat, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortKeyFloat.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Double, Object> expectedSortKeyDouble = new LinkedHashMap<>();
        expectedSortKeyDouble.put((Double) expectedArrayDouble.get(0), 1);
        expectedSortKeyDouble.put((Double) expectedArrayDouble.get(1), 2);
        expectedSortKeyDouble.put((Double) expectedArrayDouble.get(2), 3);
        expectedSortKeyDouble.put((Double) expectedArrayDouble.get(3), 4);
        actualHashMap = (LinkedHashMap) resMap.get("sortKeyDouble");
        assertEquals(expectedSortKeyDouble, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortKeyDouble.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Object, Integer> expectedSortKeyMixedNumeric = new LinkedHashMap<>();
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(0), 1);
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(1), 2);
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(2), 3);
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(3), 4);
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(4), 5);
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(5), 6);
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(6), 7);
        actualHashMap = (LinkedHashMap) resMap.get("sortKeyMixedNumeric");
        assertEquals(expectedSortKeyMixedNumeric, actualHashMap);
        expectedList = new ArrayList(expectedSortKeyMixedNumeric.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);

        LinkedHashMap<Object, Integer> expectedSortKeyMixedNumericString = new LinkedHashMap<>();
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(0), 1);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(1), 2);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(2), 3);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(3), 4);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(4), 5);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(5), 6);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(6), 7);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(7), 8);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(8), 9);
        actualHashMap = (LinkedHashMap) resMap.get("sortKeyMixedNumericString");
        assertEquals(expectedSortKeyMixedNumericString, actualHashMap);
        expectedList = new ArrayList(expectedSortKeyMixedNumericString.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        assertEquals(expectedList, actualList);
    }

    public void testExecutionHashMapSortByKeyDesc() {
        String body = "var msg = {};\n" +
                "var sortKeyString = {\"Feb\":2, \"March\":4, \"Dec\":1, \"Jan\":3, \"March\":4};\n" +
                "sortKeyString.sortByKey(false);\n" +
                "var sortKeyInt = {1:2, 57:5, 30:4, 214748:7, -214748:1, 4:3, 100000:6};\n" +
                "sortKeyInt.sortByKey(false);\n" +
                "var sortKeyIntLong = {30L:2, 1000L:5, 40L:3, -9223372036854775808L:1, 45L:4};\n" +
                "sortKeyIntLong.sortByKey(false);\n" +
                "var sortKeyFloat = {3.40282f:2, 34.175495f:3, 45.40283f:4, 1.1754943f:1};\n" +
                "sortKeyFloat.sortByKey(false);\n" +
                "var sortKeyDouble = {45.40283d:2, 9223372036854775807.17549467d:4, -922337203685.1754943d:1,1754.40282d:3};\n" +
                "sortKeyDouble.sortByKey(false);\n" +
                "var sortKeyMixedNumeric = {\"9\":4,5:2, \"8\":3, \"700\":7, 1:1, 40:5, 200:6};\n" +
                "sortKeyMixedNumeric.sortByKey(false);\n" +
                "var sortKeyMixedNumericString = {\"Babnm\":8, 200:2, 40:3, \"Zxc\":9, 5:4, \"8\":5, 1:1, \"9\":6, \"Aabnm\":7};\n" +
                "sortKeyMixedNumericString.sortByKey(false);\n" +
                "msg.sortKeyString = sortKeyString;\n" +
                "msg.sortKeyInt = sortKeyInt;\n" +
                "msg.sortKeyIntLong = sortKeyIntLong;\n" +
                "msg.sortKeyFloat = sortKeyFloat;\n" +
                "msg.sortKeyDouble = sortKeyDouble;\n" +
                "msg.sortKeyMixedNumeric = sortKeyMixedNumeric;\n" +
                "msg.sortKeyMixedNumericString = sortKeyMixedNumericString;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<String, Object> expectedSortKeyString = new LinkedHashMap<>();
        expectedSortKeyString.put(expectedArrayString.get(0), 1);
        expectedSortKeyString.put(expectedArrayString.get(1), 2);
        expectedSortKeyString.put(expectedArrayString.get(2), 3);
        expectedSortKeyString.put(expectedArrayString.get(3), 4);
        LinkedHashMap actualHashMap = (LinkedHashMap) resMap.get("sortKeyString");
        assertEquals(expectedSortKeyString, actualHashMap);
        assertEquals(expectedSortKeyString.keySet(), actualHashMap.keySet());
        ArrayList expectedList = new ArrayList<Object>(expectedSortKeyString.keySet());
        ArrayList actualList = new ArrayList<Object>(actualHashMap.keySet());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Integer, Object> expectedSortKeyInt = new LinkedHashMap<>();
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(0), 1);
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(1), 2);
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(2), 3);
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(3), 4);
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(4), 5);
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(5), 6);
        expectedSortKeyInt.put((Integer) expectedArrayInteger.get(6), 7);
        actualHashMap = (LinkedHashMap) resMap.get("sortKeyInt");
        assertEquals(expectedSortKeyInt, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortKeyInt.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Long, Object> expectedSortKeyIntLong = new LinkedHashMap<>();
        expectedSortKeyIntLong.put((Long) expectedArrayIntLong.get(0), 1);
        expectedSortKeyIntLong.put((Long) expectedArrayIntLong.get(1), 2);
        expectedSortKeyIntLong.put((Long) expectedArrayIntLong.get(2), 3);
        expectedSortKeyIntLong.put((Long) expectedArrayIntLong.get(3), 4);
        expectedSortKeyIntLong.put((Long) expectedArrayIntLong.get(4), 5);
        actualHashMap = (LinkedHashMap) resMap.get("sortKeyIntLong");
        assertEquals(expectedSortKeyIntLong, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortKeyIntLong.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Float, Object> expectedSortKeyFloat = new LinkedHashMap<>();
        expectedSortKeyFloat.put((Float) expectedArrayFloat.get(0), 1);
        expectedSortKeyFloat.put((Float) expectedArrayFloat.get(1), 2);
        expectedSortKeyFloat.put((Float) expectedArrayFloat.get(2), 3);
        expectedSortKeyFloat.put((Float) expectedArrayFloat.get(3), 4);
        actualHashMap = (LinkedHashMap) resMap.get("sortKeyFloat");
        assertEquals(expectedSortKeyFloat, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortKeyFloat.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Double, Object> expectedSortKeyDouble = new LinkedHashMap<>();
        expectedSortKeyDouble.put((Double) expectedArrayDouble.get(0), 1);
        expectedSortKeyDouble.put((Double) expectedArrayDouble.get(1), 2);
        expectedSortKeyDouble.put((Double) expectedArrayDouble.get(2), 3);
        expectedSortKeyDouble.put((Double) expectedArrayDouble.get(3), 4);
        actualHashMap = (LinkedHashMap) resMap.get("sortKeyDouble");
        assertEquals(expectedSortKeyDouble, actualHashMap);
        expectedList = new ArrayList<Object>(expectedSortKeyDouble.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Object, Integer> expectedSortKeyMixedNumeric = new LinkedHashMap<>();
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(0), 1);
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(1), 2);
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(2), 3);
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(3), 4);
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(4), 5);
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(5), 6);
        expectedSortKeyMixedNumeric.put(expectedArrayMixedNumeric.get(6), 7);
        actualHashMap = (LinkedHashMap) resMap.get("sortKeyMixedNumeric");
        assertEquals(expectedSortKeyMixedNumeric, actualHashMap);
        expectedList = new ArrayList(expectedSortKeyMixedNumeric.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);

        LinkedHashMap<Object, Integer> expectedSortKeyMixedNumericString = new LinkedHashMap<>();
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(0), 1);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(1), 2);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(2), 3);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(3), 4);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(4), 5);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(5), 6);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(6), 7);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(7), 8);
        expectedSortKeyMixedNumericString.put(expectedArrayMixedNumericString.get(8), 9);
        actualHashMap = (LinkedHashMap) resMap.get("sortKeyMixedNumericString");
        assertEquals(expectedSortKeyMixedNumericString, actualHashMap);
        expectedList = new ArrayList(expectedSortKeyMixedNumericString.values());
        actualList = new ArrayList<Object>(actualHashMap.values());
        Collections.reverse(expectedList);
        assertEquals(expectedList, actualList);
    }

    public void testExecutionHashMapToSortedByKeyAsc() {
        String body = "var msg = {};\n" +
                "var sortKeyString = {\"Feb\":2, \"March\":4, \"Dec\":1, \"Jan\":3, \"March\":4};\n" +
                "msg.toSortedKeyString = sortKeyString.toSortedByKey();\n" +
                "msg.sortKeyString = sortKeyString;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<String, Object> expectedSortKeyString = new LinkedHashMap<>();
        expectedSortKeyString.put(expectedArrayString.get(1), 2);
        expectedSortKeyString.put(expectedArrayString.get(3), 4);
        expectedSortKeyString.put(expectedArrayString.get(0), 1);
        expectedSortKeyString.put(expectedArrayString.get(2), 3);
        LinkedHashMap actualHashMap = (LinkedHashMap) resMap.get("sortKeyString");
        assertEquals(expectedSortKeyString, actualHashMap);
        assertEquals(expectedSortKeyString.keySet(), actualHashMap.keySet());
        ArrayList expectedList = new ArrayList<Object>(expectedSortKeyString.keySet());
        ArrayList actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);

        LinkedHashMap<String, Object> expectedToSortedKeyString = new LinkedHashMap<>();
        expectedToSortedKeyString.put(expectedArrayString.get(0), 1);
        expectedToSortedKeyString.put(expectedArrayString.get(1), 2);
        expectedToSortedKeyString.put(expectedArrayString.get(2), 3);
        expectedToSortedKeyString.put(expectedArrayString.get(3), 4);
        actualHashMap = (LinkedHashMap) resMap.get("toSortedKeyString");
        assertEquals(expectedToSortedKeyString, actualHashMap);
        assertEquals(expectedToSortedKeyString.keySet(), actualHashMap.keySet());
        expectedList = new ArrayList<Object>(expectedToSortedKeyString.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);
    }

    public void testExecutionHashMapToSortedByKeyDesc() {
        String body = "var msg = {};\n" +
                "var sortKeyString = {\"Feb\":2, \"March\":4, \"Dec\":1, \"Jan\":3, \"March\":4};\n" +
                "msg.toSortedKeyString = sortKeyString.toSortedByKey(false);\n" +
                "msg.sortKeyString = sortKeyString;\n" +
                "return {\n" +
                "    msg: msg\n" +
                "};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<String, Object> expectedSortKeyString = new LinkedHashMap<>();
        expectedSortKeyString.put(expectedArrayString.get(1), 2);
        expectedSortKeyString.put(expectedArrayString.get(3), 4);
        expectedSortKeyString.put(expectedArrayString.get(0), 1);
        expectedSortKeyString.put(expectedArrayString.get(2), 3);
        LinkedHashMap actualHashMap = (LinkedHashMap) resMap.get("sortKeyString");
        assertEquals(expectedSortKeyString, actualHashMap);
        assertEquals(expectedSortKeyString.keySet(), actualHashMap.keySet());
        ArrayList expectedList = new ArrayList<Object>(expectedSortKeyString.keySet());
        ArrayList actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);

        LinkedHashMap<String, Object> expectedToSortedKeyString = new LinkedHashMap<>();
        expectedToSortedKeyString.put(expectedArrayString.get(3), 4);
        expectedToSortedKeyString.put(expectedArrayString.get(2), 3);
        expectedToSortedKeyString.put(expectedArrayString.get(1), 2);
        expectedToSortedKeyString.put(expectedArrayString.get(0), 1);
        actualHashMap = (LinkedHashMap) resMap.get("toSortedKeyString");
        assertEquals(expectedToSortedKeyString, actualHashMap);
        assertEquals(expectedToSortedKeyString.keySet(), actualHashMap.keySet());
        expectedList = new ArrayList<Object>(expectedToSortedKeyString.keySet());
        actualList = new ArrayList<Object>(actualHashMap.keySet());
        assertEquals(expectedList, actualList);
    }

    public void testExecutionHashMap_invert() {
        String body = "var msg = {};\n" +
                "var map = {\"Babnm\":\"thing\", 200:2, 40:3, \"Zxc\":9, 5:\"4\", \"8\":5, 1:1, \"9\":6, \"Aabnm\":7};\n" +
                "map.invert();\n" +
                "msg.map = map;\n" +
                "return {msg: msg}";
        Object result = executeScript(body);
        LinkedHashMap<Object, Object> expectedMap = new LinkedHashMap<>();
        expectedMap.put("thing", expectedArrayMixedNumericString.get(7));
        expectedMap.put(2, expectedArrayMixedNumericString.get(1));
        expectedMap.put(3, expectedArrayMixedNumericString.get(2));
        expectedMap.put(9, expectedArrayMixedNumericString.get(8));
        expectedMap.put("4", expectedArrayMixedNumericString.get(3));
        expectedMap.put(5, expectedArrayMixedNumericString.get(4));
        expectedMap.put(1, expectedArrayMixedNumericString.get(0));
        expectedMap.put(6, expectedArrayMixedNumericString.get(5));
        expectedMap.put(7, expectedArrayMixedNumericString.get(6));
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        LinkedHashMap actualMap = (LinkedHashMap) resMap.get("map");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        boolean eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? eq : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);
    }

    public void testExecutionHashMap_invert_KeyToValueMany() {
        String body = "var msg = {};\n" +
                "var map = {\"Babnm\":\"thing\", 200:2, 40:\"thing\", \"Zxc\":9, 5:\"4\", \"8\":5, 1:\"thing\", \"9\":6, \"Aabnm\":7};\n" +
                "map.invert();\n" +
                "msg.map = map;\n" +
                "return {msg: msg}";
        Object result = executeScript(body);
        LinkedHashMap<Object, Object> expectedMap = new LinkedHashMap<>();
        expectedMap.put(2, Arrays.asList(expectedArrayMixedNumericString.get(1)));
        expectedMap.put("4", Arrays.asList(expectedArrayMixedNumericString.get(3)));
        expectedMap.put(5, Arrays.asList(expectedArrayMixedNumericString.get(4)));
        expectedMap.put(6, Arrays.asList(expectedArrayMixedNumericString.get(5)));
        expectedMap.put(7, Arrays.asList(expectedArrayMixedNumericString.get(6)));
        expectedMap.put(9, Arrays.asList(expectedArrayMixedNumericString.get(8)));
        expectedMap.put("thing", Arrays.asList(expectedArrayMixedNumericString.get(7),
                expectedArrayMixedNumericString.get(2),
                expectedArrayMixedNumericString.get(0)));
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        LinkedHashMap actualMap = (LinkedHashMap) resMap.get("map");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        boolean eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? eq : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);
    }

    public void testExecutionHashMap_toInverted() {
        String body = "var msg = {};\n" +
                "var map = {\"Babnm\":\"thing\", 200:2, 40:3, \"Zxc\":9, 5:\"4\", \"8\":5, 1:1, \"9\":6, \"Aabnm\":7};\n" +
                "msg.toInverted = map.toInverted();\n" +
                "msg.map = map;\n" +
                "return {msg: msg}";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<Object, Object> expectedMap = new LinkedHashMap<>();
        expectedMap.put(expectedArrayMixedNumericString.get(7), "thing");
        expectedMap.put(expectedArrayMixedNumericString.get(1), 2);
        expectedMap.put(expectedArrayMixedNumericString.get(2), 3);
        expectedMap.put(expectedArrayMixedNumericString.get(8), 9);
        expectedMap.put(expectedArrayMixedNumericString.get(3), "4");
        expectedMap.put(expectedArrayMixedNumericString.get(4), 5);
        expectedMap.put(expectedArrayMixedNumericString.get(0), 1);
        expectedMap.put(expectedArrayMixedNumericString.get(5), 6);
        expectedMap.put(expectedArrayMixedNumericString.get(6), 7);
        LinkedHashMap actualMap = (LinkedHashMap) resMap.get("map");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        boolean eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? true : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);

        LinkedHashMap<Object, Object> expectedInvertedMap = new LinkedHashMap<>();
        expectedInvertedMap.put("thing", expectedArrayMixedNumericString.get(7));
        expectedInvertedMap.put(2, expectedArrayMixedNumericString.get(1));
        expectedInvertedMap.put(3, expectedArrayMixedNumericString.get(2));
        expectedInvertedMap.put(9, expectedArrayMixedNumericString.get(8));
        expectedInvertedMap.put("4", expectedArrayMixedNumericString.get(3));
        expectedInvertedMap.put(5, expectedArrayMixedNumericString.get(4));
        expectedInvertedMap.put(1, expectedArrayMixedNumericString.get(0));
        expectedInvertedMap.put(6, expectedArrayMixedNumericString.get(5));
        expectedInvertedMap.put(7, expectedArrayMixedNumericString.get(6));
        actualMap = (LinkedHashMap) resMap.get("toInverted");
        assertEquals(expectedInvertedMap, actualMap);
        assertEquals(expectedInvertedMap.keySet(), actualMap.keySet());
        eq = expectedInvertedMap.values().equals(actualMap.values());
        eq = eq ? true : equalsList(expectedInvertedMap.values(), actualMap.values());
        assertTrue(eq);
    }

    public void testExecutionHashMap_toInverted_KeyToValueMany() {
        String body = "var msg = {};\n" +
                "var map = {\"Babnm\":\"thing\", 200:2, 40:\"thing\", \"Zxc\":9, 5:\"4\", \"8\":5, 1:\"thing\", \"9\":6, \"Aabnm\":7};\n" +
                "msg.toInverted = map.toInverted();\n" +
                "msg.map = map;\n" +
                "return {msg: msg}";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        LinkedHashMap<Object, Object> expectedMap = new LinkedHashMap<>();
        expectedMap.put(expectedArrayMixedNumericString.get(7), "thing");
        expectedMap.put(expectedArrayMixedNumericString.get(1), 2);
        expectedMap.put(expectedArrayMixedNumericString.get(2), "thing");
        expectedMap.put(expectedArrayMixedNumericString.get(8), 9);
        expectedMap.put(expectedArrayMixedNumericString.get(3), "4");
        expectedMap.put(expectedArrayMixedNumericString.get(4), 5);
        expectedMap.put(expectedArrayMixedNumericString.get(0), "thing");
        expectedMap.put(expectedArrayMixedNumericString.get(5), 6);
        expectedMap.put(expectedArrayMixedNumericString.get(6), 7);
        LinkedHashMap actualMap = (LinkedHashMap) resMap.get("map");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        boolean eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? true : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);

        expectedMap = new LinkedHashMap<>();
        expectedMap.put(2, Arrays.asList(expectedArrayMixedNumericString.get(1)));
        expectedMap.put("4", Arrays.asList(expectedArrayMixedNumericString.get(3)));
        expectedMap.put(5, Arrays.asList(expectedArrayMixedNumericString.get(4)));
        expectedMap.put(6, Arrays.asList(expectedArrayMixedNumericString.get(5)));
        expectedMap.put(7, Arrays.asList(expectedArrayMixedNumericString.get(6)));
        expectedMap.put(9, Arrays.asList(expectedArrayMixedNumericString.get(8)));
        expectedMap.put("thing", Arrays.asList(expectedArrayMixedNumericString.get(7),
                expectedArrayMixedNumericString.get(2),
                expectedArrayMixedNumericString.get(0)));
        actualMap = (LinkedHashMap) resMap.get("toInverted");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? eq : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);
    }

    public void testExecutionHashMap_slice() {
        String body = "var msg = {};\n" +
                "var map = {\"Babnm\":\"thing\", 200:2, 40:3, \"Zxc\":9, 5:\"4\", \"8\":5, 1:1, \"9\":6, \"Aabnm\":7};\n" +
                "msg.map = map;\n" +
                "msg.mapSlice = map.slice();\n" +
                "msg.mapSlice2 = map.slice(2);\n" +
                "msg.mapSlice2_4 = map.slice(2, 4);\n" +
                "msg.mapSlice_2 = map.slice(-2);\n" +
                "msg.mapSlice2_1 = map.slice(2, -1);\n" +
                "return {msg: msg}";
        LinkedHashMap<Object, Object> expectedMap = new LinkedHashMap<>();
        expectedMap.put(expectedArrayMixedNumericString.get(7), "thing");
        expectedMap.put(expectedArrayMixedNumericString.get(1), 2);
        expectedMap.put(expectedArrayMixedNumericString.get(2), 3);
        expectedMap.put(expectedArrayMixedNumericString.get(8), 9);
        expectedMap.put(expectedArrayMixedNumericString.get(3), "4");
        expectedMap.put(expectedArrayMixedNumericString.get(4), 5);
        expectedMap.put(expectedArrayMixedNumericString.get(0), 1);
        expectedMap.put(expectedArrayMixedNumericString.get(5), 6);
        expectedMap.put(expectedArrayMixedNumericString.get(6), 7);

        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        LinkedHashMap actualMap = (LinkedHashMap) resMap.get("map");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        boolean eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? true : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);

        actualMap = (LinkedHashMap) resMap.get("mapSlice");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? true : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);

        expectedMap = new LinkedHashMap<>();
        expectedMap.put(expectedArrayMixedNumericString.get(2), 3);
        expectedMap.put(expectedArrayMixedNumericString.get(8), 9);
        expectedMap.put(expectedArrayMixedNumericString.get(3), "4");
        expectedMap.put(expectedArrayMixedNumericString.get(4), 5);
        expectedMap.put(expectedArrayMixedNumericString.get(0), 1);
        expectedMap.put(expectedArrayMixedNumericString.get(5), 6);
        expectedMap.put(expectedArrayMixedNumericString.get(6), 7);
        actualMap = (LinkedHashMap) resMap.get("mapSlice2");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? true : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);

        expectedMap = new LinkedHashMap<>();
        expectedMap.put(expectedArrayMixedNumericString.get(2), 3);
        expectedMap.put(expectedArrayMixedNumericString.get(8), 9);
        actualMap = (LinkedHashMap) resMap.get("mapSlice2_4");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? true : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);

        expectedMap = new LinkedHashMap<>();
        expectedMap.put(expectedArrayMixedNumericString.get(5), 6);
        expectedMap.put(expectedArrayMixedNumericString.get(6), 7);
        actualMap = (LinkedHashMap) resMap.get("mapSlice_2");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? true : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);

        expectedMap = new LinkedHashMap<>();
        expectedMap.put(expectedArrayMixedNumericString.get(2), 3);
        expectedMap.put(expectedArrayMixedNumericString.get(8), 9);
        expectedMap.put(expectedArrayMixedNumericString.get(3), "4");
        expectedMap.put(expectedArrayMixedNumericString.get(4), 5);
        expectedMap.put(expectedArrayMixedNumericString.get(0), 1);
        expectedMap.put(expectedArrayMixedNumericString.get(5), 6);
        actualMap = (LinkedHashMap) resMap.get("mapSlice2_1");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? true : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);
    }

    public void testExecutionHashMap_reverse() {
        String body = "var msg = {};\n" +
                "var map = {\"Babnm\":\"thing\", 200:2, 40:3, \"Zxc\":9, 5:\"4\", \"8\":5, 1:1, \"9\":6, \"Aabnm\":7};\n" +
                "map.reverse();\n" +
                "msg.map = map;\n" +
                "return {msg: msg}";
        LinkedHashMap<Object, Object> expectedMap = new LinkedHashMap<>();
        expectedMap.put(expectedArrayMixedNumericString.get(6), 7);
        expectedMap.put(expectedArrayMixedNumericString.get(5), 6);
        expectedMap.put(expectedArrayMixedNumericString.get(0), 1);
        expectedMap.put(expectedArrayMixedNumericString.get(4), 5);
        expectedMap.put(expectedArrayMixedNumericString.get(3), "4");
        expectedMap.put(expectedArrayMixedNumericString.get(8), 9);
        expectedMap.put(expectedArrayMixedNumericString.get(2), 3);
        expectedMap.put(expectedArrayMixedNumericString.get(1), 2);
        expectedMap.put(expectedArrayMixedNumericString.get(7), "thing");
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        LinkedHashMap actualMap = (LinkedHashMap) resMap.get("map");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        boolean eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? true : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);
    }

    public void testExecutionHashMap_ToReversed() {
        String body = "var msg = {};\n" +
                "var map = {\"Babnm\":\"thing\", 200:2, 40:3, \"Zxc\":9, 5:\"4\", \"8\":5, 1:1, \"9\":6, \"Aabnm\":7};\n" +
                "msg.toReversed = map.toReversed();\n" +
                "msg.map = map;\n" +
                "return {msg: msg}";
        LinkedHashMap<Object, Object> expectedMap = new LinkedHashMap<>();
        expectedMap.put(expectedArrayMixedNumericString.get(7), "thing");
        expectedMap.put(expectedArrayMixedNumericString.get(1), 2);
        expectedMap.put(expectedArrayMixedNumericString.get(2), 3);
        expectedMap.put(expectedArrayMixedNumericString.get(8), 9);
        expectedMap.put(expectedArrayMixedNumericString.get(3), "4");
        expectedMap.put(expectedArrayMixedNumericString.get(4), 5);
        expectedMap.put(expectedArrayMixedNumericString.get(0), 1);
        expectedMap.put(expectedArrayMixedNumericString.get(5), 6);
        expectedMap.put(expectedArrayMixedNumericString.get(6), 7);

        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        LinkedHashMap actualMap = (LinkedHashMap) resMap.get("map");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        boolean eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? true : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);

        expectedMap = new LinkedHashMap<>();
        expectedMap.put(expectedArrayMixedNumericString.get(6), 7);
        expectedMap.put(expectedArrayMixedNumericString.get(5), 6);
        expectedMap.put(expectedArrayMixedNumericString.get(0), 1);
        expectedMap.put(expectedArrayMixedNumericString.get(4), 5);
        expectedMap.put(expectedArrayMixedNumericString.get(3), "4");
        expectedMap.put(expectedArrayMixedNumericString.get(8), 9);
        expectedMap.put(expectedArrayMixedNumericString.get(2), 3);
        expectedMap.put(expectedArrayMixedNumericString.get(1), 2);
        expectedMap.put(expectedArrayMixedNumericString.get(7), "thing");
        actualMap = (LinkedHashMap) resMap.get("toReversed");
        assertEquals(expectedMap, actualMap);
        assertEquals(expectedMap.keySet(), actualMap.keySet());
        eq = expectedMap.values().equals(actualMap.values());
        eq = eq ? true : equalsList(expectedMap.values(), actualMap.values());
        assertTrue(eq);
    }

    // array

    public void testCreateSingleValueArray() {
        Object res = executeScript("m = {5}; m");
        assertTrue(res instanceof List);
        assertEquals(1, ((List) res).size());
        assertEquals(5, ((List) res).get(0));
    }

    public void testExecutionArrayListToString() {
        String body = "var list = ['hello', 34567];\n" +
                "var res = '' + list;\n" +
                "return res;";
        Object result = executeScript(body);
        assertTrue(result instanceof String);
        assertEquals("[hello, 34567]", result);
    }

    public void testExecutionArrayListSortAsc() {
        String body = "var msg = {};\n" +
                "var arrayString = ['March', 'Feb', 'Jan', 'Dec'];\n" +
                "arrayString.sort();\n" +
                "var arrayInt = [1, 30, 4, -214748, 57, 214748, 100000];\n" +
                "arrayInt.sort();\n" +
                "var arrayLong = [45l, 9223372036854775807l, 30l, 40l,  -9223372036854775808l, 1000l];\n" +
                "arrayLong.sort();\n" +
                "var arrayFloat = [3.40282F, 34.175495F, 45.40283F, 1.1754943F];\n" +
                "arrayFloat.sort();\n" +
                "var arrayDouble = [45.40283d, -9.223372036851755E11d, 9223372036854775807.17549467d, 1754.40282d];\n" +
                "arrayDouble.sort();\n" +
                "var mixedNumericArray = [\"8\", \"9\", \"700\", 40, 1, 5, 200];\n" +
                "mixedNumericArray.sort();\n" +
                "msg.arrayString = arrayString;\n" +
                "msg.arrayInt = arrayInt;\n" +
                "msg.arrayLong = arrayLong;\n" +
                "msg.arrayFloat = arrayFloat;\n" +
                "msg.arrayDouble = arrayDouble;\n" +
                "msg.mixedNumericArray = mixedNumericArray;\n" +
                "var mixedNumericString = [\"8\", \"Zxc\", \"9\", 'Babnm',  40, \"Aabnm\", 1, 5, 200];\n" +
                "mixedNumericString.sort();\n" +
                "msg.mixedNumericString = mixedNumericString;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        List actualArray = (List) resMap.get("arrayString");
        assertEquals(expectedArrayString, actualArray);

        actualArray = (List) resMap.get("arrayInt");
        assertEquals(expectedArrayInteger, actualArray);

        actualArray = (List) resMap.get("arrayLong");
        assertEquals(expectedArrayIntLong, actualArray);

        actualArray = (List) resMap.get("arrayFloat");
        assertEquals(expectedArrayFloat, actualArray);

        actualArray = (List) resMap.get("arrayDouble");
        assertEquals(expectedArrayDouble, actualArray);

        actualArray = (List) resMap.get("mixedNumericArray");
        assertEquals(expectedArrayMixedNumeric, actualArray);

        actualArray = (List) resMap.get("mixedNumericString");
        assertEquals(expectedArrayMixedNumericString, actualArray);
    }

    public void testExecutionArrayListSortDesc() {
        String body = "var msg = {};\n" +
                "var arrayString = ['March', 'Feb', 'Jan', 'Dec'];\n" +
                "arrayString.sort(false);\n" +
                "var arrayInt = [1, 30, 4, -214748, 57, 214748, 100000];\n" +
                "arrayInt.sort(false);\n" +
                "var arrayLong = [45l, 9223372036854775807l, 30l, 40l,  -9223372036854775808l, 1000l];\n" +
                "arrayLong.sort(false);\n" +
                "var arrayFloat = [3.40282f, 34.175495f, 45.40283f, 1.1754943f];\n" +
                "arrayFloat.sort(false);\n" +
                "var arrayDouble = [45.40283d, 9223372036854775807.17549467d, 1754.40282d, -922337203685.1754943d];\n" +
                "arrayDouble.sort(false);\n" +
                "var mixedNumericArray = [\"8\", \"9\", \"700\", 40, 1, 5, 200];\n" +
                "msg.mixedNumericArray = mixedNumericArray;\n" +
                "var mixedNumericStringArray = [\"8\", \"Zxc\", \"9\", 'Babnm',  40, \"Aabnm\", 1, 5, 200];\n" +
                "mixedNumericStringArray.sort(false);\n" +
                "mixedNumericArray.sort(false);\n" +
                "msg.arrayString = arrayString;\n" +
                "msg.arrayInt = arrayInt;\n" +
                "msg.arrayLong = arrayLong;\n" +
                "msg.arrayFloat = arrayFloat;\n" +
                "msg.arrayDouble = arrayDouble;\n" +
                "msg.mixedNumericStringArray = mixedNumericStringArray;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");

        Collections.reverse(expectedArrayString);
        List actualArray = (List) resMap.get("arrayString");
        assertEquals(expectedArrayString, actualArray);

        Collections.reverse(expectedArrayInteger);
        actualArray = (List) resMap.get("arrayInt");
        assertEquals(expectedArrayInteger, actualArray);

        Collections.reverse(expectedArrayIntLong);
        actualArray = (List) resMap.get("arrayLong");
        assertEquals(expectedArrayIntLong, actualArray);

        Collections.reverse(expectedArrayFloat);
        actualArray = (List) resMap.get("arrayFloat");
        assertEquals(expectedArrayFloat, actualArray);

        Collections.reverse(expectedArrayDouble);
        actualArray = (List) resMap.get("arrayDouble");
        assertEquals(expectedArrayDouble, actualArray);

        Collections.reverse(expectedArrayMixedNumeric);
        actualArray = (List) resMap.get("mixedNumericArray");
        assertEquals(expectedArrayMixedNumeric, actualArray);

        Collections.reverse(expectedArrayMixedNumericString);
        actualArray = (List) resMap.get("mixedNumericStringArray");
        assertEquals(expectedArrayMixedNumericString, actualArray);
    }

    public void testExecutionArrayList_toSorted() {
        String body = "var msg = {};\n" +
                "var array = [\"Babnm\", 5, \"8\", \"9\", \"Aabnm\", 1, 200, 40, \"Zxc\"];\n" +
                "var arraySortAsc = array.toSorted();\n" +
                "var arraySortDesc = array.toSorted(false);\n" +
                "msg.array = array;\n" +
                "msg.arraySortAsc = arraySortAsc;\n" +
                "msg.arraySortDesc = arraySortDesc;\n" +
                "return {msg: msg}";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List actualArray = (List) resMap.get("array");
        List expectedArray = Arrays.asList("Babnm", 5, "8", "9", "Aabnm", 1, 200, 40, "Zxc");
        assertEquals(expectedArray, actualArray);
        actualArray = (List) resMap.get("arraySortAsc");
        expectedArray.sort(stringCompAsc);
        assertEquals(expectedArray, actualArray);
        actualArray = (List) resMap.get("arraySortDesc");
        expectedArray.sort(stringCompDesc);
        assertEquals(expectedArray, actualArray);
    }

    public void testExecutionArrayList_Reverse() {
        String body = "var msg = {};\n" +
                "var array = [\"8\", 40, 9223372036854775807, \"Dec\", \"-9223372036854775808\"];\n" +
                "msg.array = array.slice();\n" +
                "array.reverse();\n" +
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

    public void testExecutionArrayList_shift() {
        String body = "var msg = {};\n" +
                "var array = [\"8\", 40, 9223372036854775807, \"Dec\", \"-9223372036854775808\"];\n" +
                "msg.arraySlice = array.slice();\n" +
                "msg.arrayShift = array.shift();\n" +
                "msg.array = array;\n" +
                "return {msg: msg}";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        Object actualShift = resMap.get("arrayShift");
        List actualArraySlice = (List) resMap.get("arraySlice");
        List actualArrayShift = (List) resMap.get("array");
        assertEquals(actualArraySlice.get(0), actualShift);
        actualArraySlice.remove(0);
        assertEquals(actualArraySlice, actualArrayShift);
    }

    public void testExecutionArrayList_unshift() {
        String body = "var msg = {};\n" +
                "var array = [\"8\", 40, 9223372036854775807, \"Dec\", \"-9223372036854775808\"];\n" +
                "msg.arraySlice = array.slice();\n" +
                "array.unshift(42, \"Dec\");\n" +
                "msg.array = array;\n" +
                "return {msg: msg}";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List actualArraySlice = (List) resMap.get("arraySlice");
        List actualArrayShift = (List) resMap.get("array");
        actualArraySlice.add(0, "Dec");
        actualArraySlice.add(0, 42);
        assertEquals(actualArraySlice, actualArrayShift);
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

    public void testExecutionArrayList_concat() {
        String body = "var msg = {};\n" +
                "var letters = [\"Dec\", \"Feb\", \"Jan\"];\n" +
                "var numbers = [1, 2, 3];\n" +
                "var alphaNumeric = letters.concat(numbers);\n" +
                "msg.letters = letters;\n" +
                "msg.numbers = numbers;\n" +
                "msg.alphaNumeric = alphaNumeric;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        List expectedArrayString = new ArrayList();
        expectedArrayString.add("Dec");
        expectedArrayString.add("Feb");
        expectedArrayString.add("Jan");
        List expectedArrayNumbers = new ArrayList();
        expectedArrayNumbers.add(1);
        expectedArrayNumbers.add(2);
        expectedArrayNumbers.add(3);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List actualArray = (List) resMap.get("letters");
        assertEquals(expectedArrayString, actualArray);
        actualArray = (List) resMap.get("numbers");
        assertEquals(expectedArrayNumbers, actualArray);
        expectedArrayString.addAll(expectedArrayNumbers);
        actualArray = (List) resMap.get("alphaNumeric");
        assertEquals(expectedArrayString, actualArray);
    }

    public void testExecutionArrayList_splice_Inserts_index_1() {
        String body = "var msg = {};\n" +
                "var months = ['Jan', 'March', 'April', 'June'];\n" +
                "var removed = months.splice(1, 0, 'Feb');\n" +
                "msg.months = months;\n" +
                "msg.removed = removed;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add("Jan");
        expectedArray.add("Feb");
        expectedArray.add("March");
        expectedArray.add("April");
        expectedArray.add("June");
        List actualArray = (List) resMap.get("removed");
        assertEquals(0, actualArray.size());
        actualArray = (List) resMap.get("months");
        assertEquals(expectedArray, actualArray);
    }

    public void testExecutionArrayList_splice_Remove_zero_Insert_2_element_at_index_4() {
        String body = "var msg = {};\n" +
                "var months = [\"angel\", \"clown\", \"mandarin\", \"sturgeon\"];\n" +
                "var removed = months.splice(2, 0, \"drum\", \"guitar\");\n" +
                "msg.months = months;\n" +
                "msg.removed = removed;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add("angel");
        expectedArray.add("clown");
        expectedArray.add("drum");
        expectedArray.add("guitar");
        expectedArray.add("mandarin");
        expectedArray.add("sturgeon");
        List expectedArrayRemoved = new ArrayList();
        expectedArrayRemoved.add("June");
        List actualArray = (List) resMap.get("removed");
        assertEquals(0, actualArray.size());
        actualArray = (List) resMap.get("months");
        assertEquals(expectedArray, actualArray);
    }

    public void testExecutionArrayList_splice_Remove_2_Insert_3_element_at_index_1() {
        String body = "var msg = {};\n" +
                "var months = [\"angel\", \"clown\", \"trumpet\", \"sturgeon\"];\n" +
                "var removed = months.splice(1, 2, \"parrot\", \"anemone\", \"blue\");\n" +
                "msg.months = months;\n" +
                "msg.removed = removed;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add("angel");
        expectedArray.add("parrot");
        expectedArray.add("anemone");
        expectedArray.add("blue");
        expectedArray.add("sturgeon");
        List expectedArrayRemoved = new ArrayList();
        expectedArrayRemoved.add("clown");
        expectedArrayRemoved.add("trumpet");
        List actualArray = (List) resMap.get("removed");
        assertEquals(expectedArrayRemoved, actualArray);
        actualArray = (List) resMap.get("months");
        assertEquals(expectedArray, actualArray);
    }

    public void testExecutionArrayList_splice_Replaces_2_element_at_index_4() {
        String body = "var msg = {};\n" +
                "var months = [\"Jan\", \"Feb\", \"March\", \"April\", \"June\"];\n" +
                "var removed = months.splice(4, 1, 'May');\n" +
                "msg.months = months;\n" +
                "msg.removed = removed;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add("Jan");
        expectedArray.add("Feb");
        expectedArray.add("March");
        expectedArray.add("April");
        expectedArray.add("May");
        List expectedArrayRemoved = new ArrayList();
        expectedArrayRemoved.add("June");
        List actualArray = (List) resMap.get("removed");
        assertEquals(expectedArrayRemoved, actualArray);
        actualArray = (List) resMap.get("months");
        assertEquals(expectedArray, actualArray);
    }

    public void testExecutionArrayList_splice_Remove_1_element_at_index_Minus_3() {
        String body = "var msg = {};\n" +
                "var months = [\"angel\", \"clown\", \"mandarin\", \"sturgeon\"];\n" +
                "var removed = months.splice(-3, 1);\n" +
                "msg.months = months;\n" +
                "msg.removed = removed;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add("angel");
        expectedArray.add("mandarin");
        expectedArray.add("sturgeon");
        List expectedArrayRemoved = new ArrayList();
        expectedArrayRemoved.add("clown");
        List actualArray = (List) resMap.get("removed");
        assertEquals(expectedArrayRemoved, actualArray);
        actualArray = (List) resMap.get("months");
        assertEquals(expectedArray, actualArray);
    }

    public void testExecutionArrayList_splice_Remove_All_element_at_index_3() {
        String body = "var msg = {};\n" +
                "var months = [\"angel\", \"clown\", \"mandarin\", \"sturgeon\"];\n" +
                "var removed = months.splice(3);\n" +
                "msg.months = months;\n" +
                "msg.removed = removed;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add("angel");
        expectedArray.add("clown");
        expectedArray.add("mandarin");
        List actualArray = (List) resMap.get("months");
        assertEquals(expectedArray, actualArray);
        List expectedArrayRemoved = new ArrayList();
        expectedArrayRemoved.add("sturgeon");
        actualArray = (List) resMap.get("removed");
        assertEquals(expectedArrayRemoved, actualArray);

    }

    public void testExecutionArrayList_splice_Remove_All_element_at_index_Minus_3() {
        String body = "var msg = {};\n" +
                "var months = [\"angel\", \"clown\", \"mandarin\", \"sturgeon\"];\n" +
                "var removed = months.splice(-3);\n" +
                "msg.months = months;\n" +
                "msg.removed = removed;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArrayMonth = new ArrayList();
        expectedArrayMonth.add("angel");
        List expectedArrayRemoved = new ArrayList();
        expectedArrayRemoved.add("clown");
        expectedArrayRemoved.add("mandarin");
        expectedArrayRemoved.add("sturgeon");
        List actualArray = (List) resMap.get("removed");
        assertEquals(expectedArrayRemoved, actualArray);
        actualArray = (List) resMap.get("months");
        assertEquals(expectedArrayMonth, actualArray);
    }

    public void testExecutionArrayList_splice_Return_Clone() {
        String body = "var msg = {};\n" +
                "var months = [\"angel\", \"clown\", \"mandarin\", \"sturgeon\"];\n" +
                "var removed = months.splice(-3);\n" +
                "msg.months = months;\n" +
                "msg.removed = removed;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArrayMonth = new ArrayList();
        expectedArrayMonth.add("angel");
        List expectedArrayRemoved = new ArrayList();
        expectedArrayRemoved.add("clown");
        expectedArrayRemoved.add("mandarin");
        expectedArrayRemoved.add("sturgeon");
        List actualArray = (List) resMap.get("removed");
        assertEquals(expectedArrayRemoved, actualArray);
        actualArray = (List) resMap.get("months");
        assertEquals(expectedArrayMonth, actualArray);
    }

    public void testExecutionArrayList_splice_Remove_All_element_at_index_Minus_5_Size_4() {
        String body = "var msg = {};\n" +
                "var months = [\"angel\", \"clown\", \"mandarin\", \"sturgeon\"];\n" +
                "var removed = months.splice(-5);\n" +
                "msg.months = months;\n" +
                "msg.removed = removed;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArrayRemoved = new ArrayList();
        expectedArrayRemoved.add("angel");
        expectedArrayRemoved.add("clown");
        expectedArrayRemoved.add("mandarin");
        expectedArrayRemoved.add("sturgeon");
        List actualArray = (List) resMap.get("removed");
        assertEquals(expectedArrayRemoved, actualArray);
        actualArray = (List) resMap.get("months");
        assertEquals(0, actualArray.size());
    }

    public void testExecutionArrayList_toSpliced_Inserts_Element_1_index_1() {
        String body = "var msg = {};\n" +
                "var months = ['Jan', 'March', 'April', 'June'];\n" +
                "var newMonths = months.toSpliced(1, 0, 'Feb');\n" +
                "msg.months = months;\n" +
                "msg.newMonths = newMonths;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add("Jan");
        expectedArray.add("March");
        expectedArray.add("April");
        expectedArray.add("June");
        List expectedNewArray = new ArrayList();
        expectedNewArray.add("Jan");
        expectedNewArray.add("Feb");
        expectedNewArray.add("March");
        expectedNewArray.add("April");
        expectedNewArray.add("June");
        List actualArray = (List) resMap.get("months");
        assertEquals(expectedArray, actualArray);
        actualArray = (List) resMap.get("newMonths");
        assertEquals(expectedNewArray, actualArray);
    }

    public void testExecutionArrayList_toSpliced_Delete_Element_2_index_1() {
        String body = "var msg = {};\n" +
                "var months = [\"Jan\", \"Feb\", \"March\", \"Apr\", \"May\"];\n" +
                "var newMonths = months.toSpliced(1, 2);\n" +
                "msg. months =  months;\n" +
                "msg. newMonths =  newMonths;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add("Jan");
        expectedArray.add("Feb");
        expectedArray.add("March");
        expectedArray.add("Apr");
        expectedArray.add("May");
        List expectedNewArray = new ArrayList();
        expectedNewArray.add("Jan");
        expectedNewArray.add("Apr");
        expectedNewArray.add("May");
        List actualArray = (List) resMap.get("months");
        assertEquals(expectedArray, actualArray);
        actualArray = (List) resMap.get("newMonths");
        assertEquals(expectedNewArray, actualArray);
    }

    public void testExecutionArrayList_toSpliced_Complex() {
        String body = "var msg = {};\n" +
                "var months = [\"Jan\", \"Mar\", \"Apr\", \"May\"];\n" +
                "var months2 = months.toSpliced(1, 0, \"Feb\");\n" +
                "msg.months2 = months2;\n" +
                "var months3 = months2.toSpliced(2, 2);\n" +
                "msg.months3 = months3;\n" +
                "var months4 = months3.toSpliced(1, 1, \"Feb\", \"Mar\");\n" +
                "msg.months4 = months4; \n" +
                "msg.months = months; \n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add("Jan");
        expectedArray.add("Mar");
        expectedArray.add("Apr");
        expectedArray.add("May");
        // Inserting an element at index 1
        List expectedNewArray2 = new ArrayList();
        expectedNewArray2.add("Jan");
        expectedNewArray2.add("Feb");
        expectedNewArray2.add("Mar");
        expectedNewArray2.add("Apr");
        expectedNewArray2.add("May");
        // Deleting two elements starting from index 2
        List expectedNewArray3 = new ArrayList();
        expectedNewArray3.add("Jan");
        expectedNewArray3.add("Feb");
        expectedNewArray3.add("May");
        // Replacing one element at index 1 with two new elements
        List expectedNewArray4 = new ArrayList();
        expectedNewArray4.add("Jan");
        expectedNewArray4.add("Feb");
        expectedNewArray4.add("Mar");
        expectedNewArray4.add("May");
        List actualArray = (List) resMap.get("months");
        assertEquals(expectedArray, actualArray);
        actualArray = (List) resMap.get("months2");
        assertEquals(expectedNewArray2, actualArray);
        actualArray = (List) resMap.get("months3");
        assertEquals(expectedNewArray3, actualArray);
        actualArray = (List) resMap.get("months4");
        assertEquals(expectedNewArray4, actualArray);
    }

    public void testExecutionArrayList_With_Index_Ok() {
        String body = "var msg = {};\n" +
                "var arr = [1, 2, 3, 4, 5];\n" +
                "var arrWith = arr.with(2, 6);\n" +
                "msg.arr = arr;\n" +
                "msg.arrWith = arrWith;\n" +
                "return {msg: msg};";
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add(1);
        expectedArray.add(2);
        expectedArray.add(3);
        expectedArray.add(4);
        expectedArray.add(5);
        List actualArray = (List) resMap.get("arr");
        assertEquals(expectedArray, actualArray);
        expectedArray.add(2, 6);
        actualArray = (List) resMap.get("arrWith");
        assertEquals(expectedArray, actualArray);
    }

    public void testExecutionArrayList_With_Index_Bad() {
        String body = "var msg = {};\n" +
                "var arr = [1, 2, 3, 4, 5];\n" +
                "var arrWith = arr.with(6, 6);\n" +
                "msg.arr = arr;\n" +
                "msg.arrWith = arrWith;\n" +
                "return {msg: msg};";
        try {
            executeScript(body);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("arr.with(6, 6): Index: 6, Size: 5"));
        }
        body = "var msg = {};\n" +
                "var arr = [1, 2, 3, 4, 5];\n" +
                "var arrWith = arr.with(-6, 6);\n" +
                "msg.arr = arr;\n" +
                "msg.arrWith = arrWith;\n" +
                "return {msg: msg};";
        try {
            executeScript(body);
            fail("Should throw CompileException");
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("arr.with(-6, 6): Index: -6, Size: 5"));
        }
    }

    public void testExecutionArrayList_Fill_Index_Ok() {
        String parameter = "4";
        String body1 = "var msg = {};\n" +
                "var arr = [1, 2, 3];\n" +
                "var arrFill = arr.fill(";
        String body2 = ");\n" +
                "msg.arr = arr;\n" +
                "msg.arrFill = arrFill;\n" +
                "return {msg: msg};";
        String body = body1 + parameter + body2;
        Object result = executeScript(body);
        LinkedHashMap resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        List expectedArray = new ArrayList();
        expectedArray.add(4);
        expectedArray.add(4);
        expectedArray.add(4);
        List actualArray = (List) resMap.get("arr");
        assertEquals(expectedArray, actualArray);
        actualArray = (List) resMap.get("arrFill");
        assertEquals(expectedArray, actualArray);

        parameter = "4, 1";
        body = body1 + parameter + body2;
        expectedArray.set(0, 1);
        result = executeScript(body);
        resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        actualArray = (List) resMap.get("arr");
        assertEquals(expectedArray, actualArray);
        actualArray = (List) resMap.get("arrFill");
        assertEquals(expectedArray, actualArray);

        parameter = "4, 1, 2";
        body = body1 + parameter + body2;
        expectedArray.set(2, 3);
        result = executeScript(body);
        resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        actualArray = (List) resMap.get("arr");
        assertEquals(expectedArray, actualArray);
        actualArray = (List) resMap.get("arrFill");
        assertEquals(expectedArray, actualArray);

        parameter = "4, 1, 1";
        body = body1 + parameter + body2;
        expectedArray.set(0, 1);
        expectedArray.set(1, 2);
        result = executeScript(body);
        resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        actualArray = (List) resMap.get("arr");
        assertEquals(expectedArray, actualArray);
        actualArray = (List) resMap.get("arrFill");
        assertEquals(expectedArray, actualArray);

        parameter = "4, -3, -2";
        body = body1 + parameter + body2;
        expectedArray.set(0, 4);
        result = executeScript(body);
        resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        actualArray = (List) resMap.get("arr");
        assertEquals(expectedArray, actualArray);
        actualArray = (List) resMap.get("arrFill");
        assertEquals(expectedArray, actualArray);

        parameter = "4, 3, 5";
        body = body1 + parameter + body2;
        expectedArray.set(0, 1);
        result = executeScript(body);
        resMap = (LinkedHashMap) ((LinkedHashMap) result).get("msg");
        actualArray = (List) resMap.get("arr");
        assertEquals(expectedArray, actualArray);
        actualArray = (List) resMap.get("arrFill");
        assertEquals(expectedArray, actualArray);
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
                "[1, [12, 22, \"Things2\"], 2, \"Things\"],\n" +
                "  [4, 5, 6],\n" +
                "  245, \"Test_Join\",\n" +
                "  [7, \"8\", [13, [14, 24, \"Things4\"], 23, \"Things3\"], 9],\n" +
                "];\n" +
                "return matrix.join();";
        result = executeScript(body);
        assertTrue(result instanceof String);
        assertEquals("1,12,22,Things2,2,Things,4,5,6,245,Test_Join,7,8,13,14,24,Things4,23,Things3,9", result);
    }


    private Object executeScript(String ex) {
        Serializable compiled = compileExpression(ex, new ParserContext());
        this.currentExecutionContext = new ExecutionContext(this.parserConfig);
        return executeTbExpression(compiled, this.currentExecutionContext, new HashMap());
    }

    private <V> boolean equalsList(Collection l1, Collection l2) {
        boolean eq = l1.size() == l2.size();
        if (eq) {
            for (Object o : l1) {
                eq = l2.contains(o);
                if (!eq) break;
            }
        }
        return eq;
    }
}
