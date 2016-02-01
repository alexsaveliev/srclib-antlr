package com.sourcegraph.toolchain.clojure;

import com.sourcegraph.toolchain.clojure.antlr4.ClojureParser;
import com.sourcegraph.toolchain.language.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class NamespaceContextResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceContextResolver.class);

    public static final String NAMESPACE_SEPARATOR = "/";
    private static final String CLOJURE_DEFAULT_NAMESPACE_NAME = "user";

    private Namespace currentNamespace;
    private Map<String, Namespace> allNamespaces = new HashMap<>();

    //private static NamespaceContextResolver instance = new NamespaceContextResolver ( );

    public NamespaceContextResolver() {
        currentNamespace = new Namespace(CLOJURE_DEFAULT_NAMESPACE_NAME, this);
        allNamespaces.put(CLOJURE_DEFAULT_NAMESPACE_NAME,  currentNamespace);
    }

//    public static NamespaceContextResolver  getInstance( ) {
//        return instance;
//    }

    public Namespace getNamespaceByName(String name) {
        return allNamespaces.get(name);
    }

    public Context<Boolean> context() {
        return currentNamespace.getContext();
    }

    public void enterNamespace(String nsName) {
        allNamespaces.put(currentNamespace.getName(), currentNamespace);

        //getting previously saved context for namespace or creating new one
        Namespace savedNamespace = allNamespaces.get(nsName);
        if (savedNamespace == null) {
            currentNamespace = new Namespace(nsName, this);
        } else {
            currentNamespace = savedNamespace;
        }
    }

    public String lookup(ClojureParser.SymbolContext ctx) {

        String fullName = ctx.getText();

        //qualified symbol using lookup in user defined namespace first
        if (ctx.ns_symbol() != null) {
            String[] parts = fullName.split(NAMESPACE_SEPARATOR);
            String nsName = parts[0];
            String varName = parts[1];

            Namespace savedNamespace = allNamespaces.get(nsName);
            if (savedNamespace != null) {
                return savedNamespace.lookup(varName);
            }
        }

        //simple symbol without namespace lookup
        //or treat as external name from non-user defined context
        return currentNamespace.lookup(fullName);
    }

    public String currentNamespaceName() {
        return currentNamespace.getName();
    }

    public void addUsedNamespace(String namespace) {
        currentNamespace.addUsedNamespace(namespace);
    }

}
