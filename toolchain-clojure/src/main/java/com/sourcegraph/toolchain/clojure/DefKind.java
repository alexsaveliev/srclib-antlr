package com.sourcegraph.toolchain.clojure;

interface DefKind {

    public static final String FUNC = "func";
    public static final String PARAM = "param";
    public static final String BINDING_VAR = "binding_var";
    public static final String VAR = "var";
}
