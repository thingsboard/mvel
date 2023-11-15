package org.mvel2.execution;

import org.mvel2.ExecutionContext;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mvel2.execution.ExecutionArrayList.validateClazzInArrayIsOnlyString;

public class ExecutionHashMap<K, V> extends LinkedHashMap<K, V> implements ExecutionObject {

    private final ExecutionContext executionContext;

    private final int id;

    private long memorySize = 0;

    public ExecutionHashMap(int size, ExecutionContext executionContext) {
        super(size);
        this.executionContext = executionContext;
        this.id = executionContext.nextId();
    }

    @Override
    public V put(K key, V value) {
        if (containsKey(key)) {
            V prevValue = this.get(key);
            this.memorySize -= this.executionContext.onValRemove(this, key, prevValue);
        }
        V res;
        if (value != null) {
            res = super.put(key, value);
            this.memorySize += this.executionContext.onValAdd(this, key, value);
        } else {
            res = super.remove(key);
        }
        return res;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        super.putAll(m);
        for (Map.Entry<? extends K, ? extends V> val : m.entrySet()) {
            this.memorySize += this.executionContext.onValAdd(this, val.getKey(), val.getValue());
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {
        if (!super.containsKey(key)) {
            this.memorySize += this.executionContext.onValAdd(this, key, value);
        }
        return super.putIfAbsent(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        boolean result = super.replace(key, oldValue, newValue);
        if (result) {
            this.memorySize -= this.executionContext.onValRemove(this, key, oldValue);
            this.memorySize += this.executionContext.onValAdd(this, key, newValue);
        }
        return result;
    }

    @Override
    public V replace(K key, V value) {
        this.memorySize += this.executionContext.onValAdd(this, key, value);
        return super.replace(key, value);
    }

    @Override
    public V remove(Object key) {
        if (containsKey(key)) {
            V value = this.get(key);
            this.memorySize -= this.executionContext.onValRemove(this, key, value);
        }
        return super.remove(key);
    }

    @Override
    public int getExecutionObjectId() {
        return id;
    }

    @Override
    public long memorySize() {
        return memorySize;
    }

    @Override
    public ExecutionArrayList<V> values() {
        return new ExecutionArrayList<>(super.values(), this.executionContext);
    }

    public ExecutionArrayList<K> keys() {
        return new ExecutionArrayList<>(super.keySet(), this.executionContext);
    }

    public void sortByValue() {
        sortByValue(true);
    }


    public void sortByValue(boolean asc) {
        Map valueSort = sortMapByValue((HashMap) super.clone(), validateClazzInArrayIsOnlyString(this.values()), asc);
        valueSort.keySet().forEach(this::remove);
        this.putAll(valueSort);
    }

    public void sortByKey() {
        this.sortByKey(true);
    }

    public void sortByKey(boolean asc) {
        ExecutionArrayList keys = this.keys();
        keys.sort(asc);
        HashMap keysMapSort = new LinkedHashMap();
        keys.forEach(k -> keysMapSort.put(k, this.get(k)));
        keysMapSort.keySet().forEach(this::remove);
        this.putAll(keysMapSort);
    }

    private static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map, boolean isString, boolean asc) {
        Comparator<? super Map.Entry> cmp = isString ? compByValueString(asc) : compByValueDouble(asc);
        return map.entrySet()
                .stream()
                .sorted(cmp)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private static <K, V extends Comparable<? super V>> Comparator compByValueString(boolean asc) {
        return new Comparator() {
            public int compare(Object o1, Object o2) {
                String first = String.valueOf(((Map.Entry) o1).getValue());
                String second = String.valueOf(((Map.Entry) o2).getValue());
                return asc ? first.compareTo(second) : second.compareTo(first);
            }
        };
    }

    private static <K, V extends Comparable<? super V>> Comparator compByValueDouble(boolean asc) {
        return new Comparator() {
            public int compare(Object o1, Object o2) {
                Double first = Double.parseDouble(String.valueOf(((Map.Entry) o1).getValue()));
                Double second = Double.parseDouble(String.valueOf(((Map.Entry) o2).getValue()));
                return asc ? first.compareTo(second) : second.compareTo(first);
            }
        };
    }
}
