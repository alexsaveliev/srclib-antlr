package com.sourcegraph.toolchain.language;

public class LookupResult<E> {

    private E value;
    private Scope scope;
    private int depth;

    public LookupResult(E value, Scope scope, int depth) {
        this.value = value;
        this.scope = scope;
        this.depth = depth;
    }

    public E getValue() {
        return value;
    }

    public Scope getScope() {
        return scope;
    }

    public int getDepth() {
        return depth;
    }
}
