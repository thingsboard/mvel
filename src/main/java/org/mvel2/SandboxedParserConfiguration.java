package org.mvel2;

import org.mvel2.compiler.AbstractParser;
import org.mvel2.util.TriFunction;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SandboxedParserConfiguration extends ParserConfiguration {

    private final Map<Class<?>, Function<Object, Long>> additionalDataTypes = new HashMap<>();

    private final Map<Method, TriFunction<ExecutionContext, Object, Object[], Object[]>> invocationCheckers = new HashMap<>();

    private SandboxedClassLoader sanboxedClassLoader = new SandboxedClassLoader();

    protected static final Map<String, Object> literals = AbstractParser.LITERALS
            .entrySet().stream().filter(entry -> !SandboxedClassLoader.forbiddenClassLiterals.contains(entry.getKey()))
            .collect(HashMap::new, (m, v)->m.put(v.getKey(), v.getValue()), HashMap::putAll);

    public SandboxedParserConfiguration() {
        setClassLoader(sanboxedClassLoader);
        setImports(AbstractParser.CLASS_LITERALS
                .entrySet().stream().filter(entry -> !SandboxedClassLoader.forbiddenClassLiterals.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        this.registerDefaultInvocationCheckers();
    }

    private void registerDefaultInvocationCheckers() {
        try {
            registerMethodInvocationChecker(String.class.getMethod("repeat", int.class), (ctx, value, args) -> {
                if (ctx.getMaxAllowedMemory() > 0) {
                    if (args != null && args.length > 0 && args[0] instanceof Integer) {
                        int count = (Integer)args[0];
                        long stringSize = (long) ((String) value).length() * count;
                        if (stringSize > ctx.getMaxAllowedMemory() / 2) {
                            throw new ScriptMemoryOverflowException("Max string length overflow (" + stringSize + " > " + ctx.getMaxAllowedMemory() / 2 + ")!");
                        }
                    }
                }
                return args;
            });
            registerMethodInvocationChecker(String.class.getMethod("concat", String.class), (ctx, value, args) -> {
                if (ctx.getMaxAllowedMemory() > 0) {
                    if (args != null && args.length > 0 && args[0] instanceof String) {
                        String str = (String)args[0];
                        int stringSize = ((String) value).length() + str.length();
                        if (stringSize > ctx.getMaxAllowedMemory() / 2) {
                            throw new ScriptMemoryOverflowException("Max string length overflow (" + stringSize + " > " + ctx.getMaxAllowedMemory() / 2 + ")!");
                        }
                    }
                }
                return args;
            });
            registerMethodInvocationChecker(String.class.getMethod("replace", CharSequence.class, CharSequence.class), (ctx, value, args) -> {
                if (ctx.getMaxAllowedMemory() > 0) {
                    if (args != null && args.length > 1 && args[1] instanceof CharSequence) {
                        CharSequence replacement = (CharSequence)args[1];
                        int stringSize = replacement.length();
                        if (stringSize > ctx.getMaxAllowedMemory() / 100) {
                            throw new ScriptMemoryOverflowException("Max replacement length overflow (" + stringSize + " > " + ctx.getMaxAllowedMemory() / 10 + ")!");
                        }
                    }
                }
                return args;
            });
            registerMethodInvocationChecker(String.class.getMethod("replaceAll", String.class, String.class), (ctx, value, args) -> {
                if (ctx.getMaxAllowedMemory() > 0) {
                    if (args != null && args.length > 1 && args[1] instanceof String) {
                        String replacement = (String)args[1];
                        int stringSize = replacement.length();
                        if (stringSize > ctx.getMaxAllowedMemory() / 100) {
                            throw new ScriptMemoryOverflowException("Max replacement length overflow (" + stringSize + " > " + ctx.getMaxAllowedMemory() / 10 + ")!");
                        }
                    }
                }
                return args;
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to register default invocation checkers!", e);
        }
    }

    @Override
    public boolean hasImport(String name) {
        return imports.containsKey(name);
    }

    @Override
    public Class getImport(String name) {
        if (imports.containsKey(name) && imports.get(name) instanceof Class) {
            return (Class) imports.get(name);
        }
        return null;
    }

    @Override
    public void addImport(String name, Class cls) {
        super.addImport(name, cls);
        sanboxedClassLoader.addAllowedClass(cls);
    }
   @Override
    public void addNonConvertableClass(String clazz, Set methods) {
        super.addNonConvertableClass(clazz, methods);
    }

    @Override
    public Object getStaticOrClassImport(String name) {
        return imports.getOrDefault(name, null);
    }

    public void addAllowedPackage(String packageName) {
        super.addPackageImport(packageName);
        this.sanboxedClassLoader.addAllowedPackage(packageName);
    }

    @SuppressWarnings("unchecked")
    public <T> void registerDataType(String name, Class<T> cls, Function<T, Long> valueSizeFunction) {
        this.addImport(name, cls);
        this.additionalDataTypes.put(cls, (Function<Object, Long>) valueSizeFunction);
    }

    public void registerMethodInvocationChecker(Method method, TriFunction<ExecutionContext, Object, Object[], Object[]> methodInvocationCheckerFunction) {
        this.invocationCheckers.put(method, methodInvocationCheckerFunction);
    }

    public Function<Object, Long> getValueSizeFunction(Class<?> cls) {
        return this.additionalDataTypes.get(cls);
    }

    public TriFunction<ExecutionContext, Object, Object[], Object[]> getMethodInvocationChecker(Method method) {
        return this.invocationCheckers.get(method);
    }
}
