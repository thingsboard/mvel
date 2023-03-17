package org.mvel2;

import org.mvel2.execution.ExecutionArrayList;
import org.mvel2.execution.ExecutionObject;
import org.mvel2.util.TriFunction;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExecutionContext implements Serializable {

    private final Map<Object, ValueReference> valueReferenceMap = new IdentityHashMap<>();
    private final Map<VarKey, Object> variablesMap = new HashMap<>();

    private final SandboxedParserConfiguration parserConfig;
    private final long maxAllowedMemory;

    private final int maxAllowedMethodArgs;

    private int stackLevel = 0;

    private long memorySize = 0;

    private final AtomicInteger idSequence = new AtomicInteger(0);

    private volatile boolean stopped = false;

    public ExecutionContext(SandboxedParserConfiguration parserConfig) {
        this(parserConfig, -1);
    }

    public ExecutionContext(SandboxedParserConfiguration parserConfig, long maxAllowedMemory) {
        this(parserConfig, maxAllowedMemory, 10);
    }

    public ExecutionContext(SandboxedParserConfiguration parserConfig, long maxAllowedMemory, int maxAllowedMethodArgs) {
        this.parserConfig = parserConfig;
        this.maxAllowedMemory = maxAllowedMemory;
        this.maxAllowedMethodArgs = maxAllowedMethodArgs;
    }

    public int nextId() {
        return this.idSequence.incrementAndGet();
    }

    public void checkExecution() {
        if (stopped) {
            throw new ScriptExecutionStoppedException("Script execution is stopped!");
        }
    }

    public Object[] checkInvocation(Method method, Object ctx, Object[] args) {
        if (args != null && maxAllowedMethodArgs > 0 && args.length > maxAllowedMethodArgs) {
            throw new ScriptRuntimeException("Maximum method arguments count overflow (" + args.length + " > " + maxAllowedMethodArgs + ")!");
        }
        TriFunction<ExecutionContext, Object, Object[], Object[]> invocationChecker = this.parserConfig.getMethodInvocationChecker(method);
        if (invocationChecker != null) {
            return invocationChecker.apply(this, ctx, args);
        }
        return args;
    }

    public void stop() {
        this.stopped = true;
    }

    public void enterStack() {
        this.stackLevel++;
    }

    public void leaveStack() {
        int level = this.stackLevel;
        List<VarKey> keysToRemove = variablesMap.keySet().stream().filter(varKey -> varKey.level == level).collect(Collectors.toList());
        keysToRemove.forEach(key -> this.checkAssignVariable(key, null));
        this.stackLevel--;
    }

    public void checkArray(Class<?> componentType, int... dimensions) {
        if (componentType.isPrimitive()) {
            long arraySize = 1;
            for (int i =0; i < dimensions.length; i++) {
                arraySize *= dimensions[i];
            }
            long arrayMemorySize = arraySize * componentTypeSize(componentType);
            if (maxAllowedMemory > 0 && arrayMemorySize > maxAllowedMemory / 2) {
                throw new ScriptMemoryOverflowException("Max array length overflow (" + arrayMemorySize + " > " + maxAllowedMemory / 2 + ")!");
            }
        } else {
            throw new ScriptRuntimeException("Unsupported array type: " + componentType);
        }
    }

    public Object checkAssignGlobalVariable(String varName, Object value) {
        return this.checkAssignVariable(new VarKey(0, varName), value);
    }

    public Object checkAssignLocalVariable(String varName, Object value) {
        return this.checkAssignVariable(new VarKey(this.stackLevel, varName), value);
    }

    private Object checkAssignVariable(VarKey varKey, Object value) {
        if (this.variablesMap.containsKey(varKey)) {
            Object prevValue = this.variablesMap.get(varKey);
            ValueReference reference = valueReferenceMap.get(prevValue);
            if (reference != null) {
                if (reference.removeReference(varKey)) {
                    valueReferenceMap.remove(prevValue);
                    memorySize -= reference.getSize();
                }
            }
        }
        if (value != null) {
            Object converted = convertValue(value);
            this.variablesMap.put(varKey, converted);
            ValueReference reference = valueReferenceMap.computeIfAbsent(value, o -> {
                ValueReference newReference = new ValueReference();
                newReference.setSize(getValueSize(converted));
                memorySize += newReference.getSize();
                return newReference;
            });
            reference.addReference(varKey);
            value = converted;
        } else {
            this.variablesMap.remove(varKey);
        }
        this.checkMemoryLimit();
        return value;
    }

    public long onValRemove(ExecutionObject obj, Object key, Object val) {
        long valSize = getValueSize(key) + getValueSize(val);
        ValueReference reference = valueReferenceMap.get(obj);
        if (reference != null) {
            reference.setSize(reference.getSize() - valSize);
        }
        memorySize -= valSize;
        return valSize;
    }

    public long onValAdd(ExecutionObject obj, Object key, Object val) {
        long valSize = getValueSize(key) + getValueSize(val);
        ValueReference reference = valueReferenceMap.get(obj);
        if (reference != null) {
            reference.setSize(reference.getSize() + valSize);
        }
        memorySize += valSize;
        this.checkMemoryLimit();
        return valSize;
    }

    public void dumpVars() {
        System.out.println("VARS:");
        variablesMap.forEach((key, value) -> System.out.println(key + " = " + value));
    }

    public void dumpValueReferences() {
        System.out.println("VALUE REFERENCES:");
        valueReferenceMap.forEach((key, value) -> System.out.println(key + " = " + value));
    }

    public long getMemorySize() {
        return memorySize;
    }

    public long getMaxAllowedMemory() {
        return maxAllowedMemory;
    }

    private void checkMemoryLimit() {
        if (maxAllowedMemory > 0 && memorySize > maxAllowedMemory) {
            throw new ScriptMemoryOverflowException("Script memory overflow (" + memorySize + " > " + maxAllowedMemory + ")!");
        }
    }

    private Object convertValue(Object value) {
        if (value.getClass().isArray() && !value.getClass().getComponentType().isPrimitive()) {
            value = new ExecutionArrayList(Arrays.asList((Object[])value), this);
        }
        return value;
    }

    private long getValueSize(Object value) {
        if (value == null) {
            return 0;
        }
        Function<Object, Long> valueSizeFunction = this.parserConfig.getValueSizeFunction(value.getClass());
        if (valueSizeFunction != null) {
            return valueSizeFunction.apply(value);
        } else if (value instanceof ExecutionObject) {
            if (valueReferenceMap.containsKey(value)) {
                return 4;
            } else {
                return ((ExecutionObject) value).memorySize();
            }
        } else if (value instanceof String) {
            return ((String) value).getBytes().length;
        } else if (value instanceof Byte) {
            return 1;
        } else if (value instanceof Character) {
            return 1;
        } else if (value instanceof Short) {
            return 2;
        } else if (value instanceof Integer) {
            return 4;
        } else if (value instanceof Long) {
            return 8;
        } else if (value instanceof Float) {
            return 4;
        } else if (value instanceof Double) {
            return 8;
        } else if (value instanceof BigInteger) {
            return ((BigInteger) value).bitLength()/8 + 1;
        } else if (value instanceof Boolean) {
            return 1;
        } else if (value instanceof UUID) {
            return 16;
        } else if (value instanceof Date) {
            return 8;
        } else if (value.getClass().isArray() && value.getClass().getComponentType().isPrimitive()) {
            return (long) Array.getLength(value) * componentTypeSize(value.getClass().getComponentType());
        } else {
            throw new ScriptRuntimeException("Unsupported value type: " + value.getClass());
        }
    }

    private static int componentTypeSize(Class<?> componentType) {
        if (byte.class.equals(componentType)) {
            return 1;
        } else if (char.class.equals(componentType)) {
            return 1;
        } else if (short.class.equals(componentType)) {
            return 2;
        } else if (int.class.equals(componentType)) {
            return 4;
        } else if (long.class.equals(componentType)) {
            return 8;
        } else if (float.class.equals(componentType)) {
            return 4;
        } else if (double.class.equals(componentType)) {
            return 8;
        } else if (boolean.class.equals(componentType)) {
            return 1;
        } else {
            throw new ScriptRuntimeException("Unsupported array primitive type: " + componentType);
        }
    }

    private static final class VarKey {
        private final int level;
        private final String name;
        VarKey(int level, String name) {
            this.level = level;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VarKey varKey = (VarKey) o;
            return level == varKey.level && Objects.equals(name, varKey.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(level, name);
        }
    }

    private static final class ValueReference {
        private final Set<VarKey> references = new HashSet<>();
        private long size = 0;

        void addReference(VarKey varKey) {
            references.add(varKey);
        }

        boolean removeReference(VarKey varKey) {
            references.remove(varKey);
            return references.isEmpty();
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        @Override
        public String toString() {
            return "ValueReference[size: " + size + "; references: " + references + "]";
        }
    }
}
