package org.mvel2.execution;

import org.mvel2.ExecutionContext;

import java.util.LinkedHashMap;
import java.util.Map;

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
        if(result){
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
    public String toString() {
        String res = super.toString();
        return "(id=" + id + ") " + res;
    }
}
