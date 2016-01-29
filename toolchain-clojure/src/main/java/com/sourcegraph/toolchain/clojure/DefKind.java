package com.sourcegraph.toolchain.clojure;

interface DefKind {

    //public static final String NAMESPACE = "namespace";
    public static final String STRUCT = "struct";
    public static final String CLASS = "class";
    public static final String ENUM = "enum";
    public static final String FUNC = "func";
    public static final String PARAM = "param";
    public static final String LETVAR = "letvar";
    public static final String VAR = "var";
    public static final String CASE = "case";
}
