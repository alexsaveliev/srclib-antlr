package com.sourcegraph.toolchain.clojure;

interface DefKind {

    String FUNC = "func";
    String PARAM = "param";
    String BINDING_VAR = "binding_var";
    String VAR = "var";
}
