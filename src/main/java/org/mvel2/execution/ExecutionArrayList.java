package org.mvel2.execution;

import org.mvel2.ExecutionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ExecutionArrayList<E> extends ArrayList<E> implements ExecutionObject {

    private static final Comparator stringCompDesc = new Comparator() {
        public int compare(Object o1, Object o2) {
            String first = String.valueOf(o1);
            String second = String.valueOf(o2);
            return second.compareTo(first);
        }
    };
    private static final Comparator numericCompAsc = new Comparator() {
        public int compare(Object o1, Object o2) {
            Double first = Double.parseDouble(String.valueOf(o1));
            Double second = Double.parseDouble(String.valueOf(o2));
            return first.compareTo(second);
        }
    };
    private static final Comparator numericCompDesc = new Comparator() {
        public int compare(Object o1, Object o2) {
            Double first = Double.parseDouble(String.valueOf(o1));
            Double second = Double.parseDouble(String.valueOf(o2));
            return second.compareTo(first);
        }
    };

    private final ExecutionContext executionContext;

    private final int id;

    private long memorySize = 0;

    public ExecutionArrayList(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        this.id = executionContext.nextId();
    }

    public ExecutionArrayList(Collection<? extends E> c, ExecutionContext executionContext) {
        super(c);
        this.executionContext = executionContext;
        this.id = executionContext.nextId();
        for (int i = 0; i < size(); i++) {
            E val = get(i);
            this.memorySize += this.executionContext.onValAdd(this, i, val);
        }
    }

    public boolean push(E e) {
        return this.add(e);
    }

    public E pop() {
        int size = size();
        if (size == 0) {
            return null;
        } else {
            return this.remove(size - 1);
        }
    }

    public E shift() {
        return remove(0);
    }

    public void unshift(E e) {
        add(0, e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean res = super.addAll(c);
        int i = c.size();
        for (E val : c) {
            this.memorySize += this.executionContext.onValAdd(this, i++, val);
        }
        return res;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        boolean res = super.addAll(index, c);
        int i = index;
        for (E val : c) {
            this.memorySize += this.executionContext.onValAdd(this, i++, val);
        }
        return res;
    }

    @Override
    public void add(int index, E e) {
        super.add(index, e);
        this.memorySize += this.executionContext.onValAdd(this, index, e);
    }

    @Override
    public boolean add(E e) {
        boolean res = super.add(e);
        this.memorySize += this.executionContext.onValAdd(this, size() - 1, e);
        return res;
    }

    @Override
    public E remove(int index) {
        E value = super.remove(index);
        this.memorySize -= this.executionContext.onValRemove(this, index, value);
        return value;
    }

    @Override
    public boolean remove(Object value) {
        int index = super.indexOf(value);
        if (index >= 0) {
            this.remove(index);
            return true;
        }
        return false;
    }

    @Override
    public E set(int index, E element) {
        E oldValue = super.set(index, element);
        this.memorySize -= this.executionContext.onValRemove(this, index, oldValue);
        this.memorySize += this.executionContext.onValAdd(this, index, element);
        return oldValue;
    }

    public ExecutionArrayList<E> slice(int start, int end) {
        return new ExecutionArrayList<>(this.subList(start, end), this.executionContext);
    }

    public int length() {
        return size();
    }

    @Override
    public int getExecutionObjectId() {
        return id;
    }

    @Override
    public long memorySize() {
        return memorySize;
    }

    public String join() {
        return join(",");
    }

    public String join(String separator) {
        return this.stream()
                .map(Object::toString)
                .collect(Collectors.joining(separator));
    }

    public void sort() {
        this.sort(true);
    }

    public void sort(boolean asc) {
        if (validateClazzInArrayIsOnlyString()) {
            if (asc) {
                super.sort(null);
            } else {
                super.sort(stringCompDesc);
            }
        } else {
            if (asc) {
                super.sort(numericCompAsc);
            } else {
                super.sort(numericCompDesc);
            }
        }
    }

    public void toReversed() {
        List listRev = (List) super.clone();
        Collections.reverse(listRev);
        listRev.forEach(e -> remove(e));
        addAll(listRev);
    }

    public boolean validateClazzInArrayIsOnlyString() {
        Optional simpleNamOpt = super.stream().filter(e -> !(e instanceof String)).findAny();
        return !simpleNamOpt.isPresent();
    }
}
