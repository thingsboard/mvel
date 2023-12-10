package org.mvel2.execution;

import org.mvel2.ExecutionContext;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mvel2.util.ArrayTools.initEndIndex;
import static org.mvel2.util.ArrayTools.initStartIndex;

public class ExecutionHashMap<K, V> extends LinkedHashMap<K, V> implements ExecutionObject {

    private static final Comparator compByValueStringAsc = new Comparator() {
        public int compare(Object o1, Object o2) {
            String first = String.valueOf(((Map.Entry) o1).getValue());
            String second = String.valueOf(((Map.Entry) o2).getValue());
            return first.compareTo(second);
        }
    };
    private static final Comparator compByValueStringDesc = new Comparator() {
        public int compare(Object o1, Object o2) {
            String first = String.valueOf(((Map.Entry) o1).getValue());
            String second = String.valueOf(((Map.Entry) o2).getValue());
            return second.compareTo(first);
        }
    };

    private static final Comparator compByValueDoubleAsc = new Comparator() {
        public int compare(Object o1, Object o2) {
            Double first = Double.parseDouble(String.valueOf(((Map.Entry) o1).getValue()));
            Double second = Double.parseDouble(String.valueOf(((Map.Entry) o2).getValue()));
            return first.compareTo(second);
        }
    };

    private static final Comparator compByValueDoubleDesc = new Comparator() {
        public int compare(Object o1, Object o2) {
            Double first = Double.parseDouble(String.valueOf(((Map.Entry) o1).getValue()));
            Double second = Double.parseDouble(String.valueOf(((Map.Entry) o2).getValue()));
            return second.compareTo(first);
        }
    };

    private final ExecutionContext executionContext;

    private final int id;

    private long memorySize = 0;

    public ExecutionHashMap(int size, ExecutionContext executionContext) {
        super(size);
        this.executionContext = executionContext;
        this.id = executionContext.nextId();
    }

    public ExecutionHashMap(Map<K, V> map, ExecutionContext executionContext) {
        super(map.size());
        this.executionContext = executionContext;
        this.id = executionContext.nextId();
        this.putAll(map);
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

    public ExecutionHashMap<K, V> slice() {
        return (ExecutionHashMap<K, V>) this.clone();
    }

    public ExecutionHashMap<K, V> slice(int start) {
        return slice(start, this.size());
    }

    public ExecutionHashMap<K, V> slice(int start, int end) {
        List keys = this.keys();
        start = initStartIndex(start, keys);
        end = initEndIndex(end, keys);
        ExecutionHashMap<K, V> mapSlice = new ExecutionHashMap<>(end - start, this.executionContext);
        int index = 0;
        for (Map.Entry<K, V> entry : this.entrySet()) {
            if (index >= start && index < end) {
                mapSlice.put(entry.getKey(), entry.getValue());
            }
            ++index;
        }
        return mapSlice;
    }

    public void sortByValue() {
        this.sortByValue(true);
    }

    public void sortByValue(boolean asc) {
        Map valueSort = sortMapByValue((HashMap) super.clone(), asc);
        this.clearAllPutAll(valueSort);
    }

    public void sortByKey() {
        this.sortByKey(true);
    }

    public void sortByKey(boolean asc) {
        ExecutionArrayList keys = this.keys();
        keys.sort(asc);
        HashMap keysMapSort = new LinkedHashMap();
        keys.forEach(k -> keysMapSort.put(k, this.get(k)));
        this.clearAllPutAll(keysMapSort);
    }

    public ExecutionHashMap<K, V> toSortedByValue() {
        return this.toSortedByValue(true);
    }

    public ExecutionHashMap<K, V> toSortedByValue(boolean asc) {
        Map valueToSorted = sortMapByValue((HashMap) super.clone(), asc);
        return new ExecutionHashMap<>(valueToSorted, this.executionContext);
    }

    public ExecutionHashMap<K, V> toSorted() {
        return this.toSorted(true);
    }

    public ExecutionHashMap<K, V> toSorted(boolean asc) {
        return this.toSortedByKey(asc);
    }

    public ExecutionHashMap<K, V> toSortedByKey() {
        return this.toSortedByKey(true);
    }

    public ExecutionHashMap<K, V> toSortedByKey(boolean asc) {
        ExecutionArrayList keysToSorted = this.keys();
        keysToSorted.sort(asc);
        ExecutionHashMap<K, V> mapToSortedByKey = new ExecutionHashMap<>(this.size(), this.executionContext);
        keysToSorted.forEach(k -> mapToSortedByKey.put((K) k, this.get(k)));
        return mapToSortedByKey;
    }

    public void invert() {
        Map<K, V> mapClone = (Map<K, V>) super.clone();
        Map mapInvert = new LinkedHashMap<>();
        mapClone.forEach((key, value) -> mapInvert.put(value, key));
        super.clear();
        super.putAll(mapInvert);
    }

    public ExecutionHashMap<K, V> toInverted() {
        Map<K, V> mapClone = (Map<K, V>) super.clone();
        Map mapInverted = new LinkedHashMap<>();
        mapClone.forEach((key, value) -> mapInverted.put(value, key));
        return new ExecutionHashMap<>(mapInverted, this.executionContext);
    }

    public void reverse() {
        this.clearAllPutAll(this.reversByKeys());
    }

    public ExecutionHashMap<K, V> toReverse() {
        return new ExecutionHashMap<>(this.reversByKeys(), this.executionContext);
    }


    private <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map, boolean asc) {
        Comparator<? super Map.Entry> cmp;
        if (this.values().validateClazzInArrayIsOnlyNumber()) {
            cmp = asc ? compByValueDoubleAsc : compByValueDoubleDesc;
        } else {
            cmp = asc ? compByValueStringAsc : compByValueStringDesc;
        }
        return map.entrySet()
                .stream()
                .sorted(cmp)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private void clearAllPutAll(Map<? extends K, ? extends V> m) {
        if (this.size() == m.size()) {
            super.clear();
            super.putAll(m);
        } else {
            throw new IllegalArgumentException("Input map.size() is not equal to this.size()!");
        }
    }

    private HashMap reversByKeys() {
        ExecutionArrayList keys = this.keys();
        keys.reverse();
        HashMap keysMapRevers = new LinkedHashMap();
        keys.forEach(k -> keysMapRevers.put(k, this.get(k)));
        return keysMapRevers;
    }
}
