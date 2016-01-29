package com.sourcegraph.toolchain.clojure;

import com.sourcegraph.toolchain.clojure.antlr4.ClojureParser;
import com.sourcegraph.toolchain.language.Context;
import com.sourcegraph.toolchain.language.LookupResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamespaceContextResolver {

    private class Namespace {

        public String name;
        public Context<Boolean> context;
        public List<Namespace> usedNamespaces;
        //public List<String> usedDefs;
        //public List<String> excludedDefs;

        public Namespace(String name) {
            this.name = name;
            context = new Context<>();
            usedNamespaces = new ArrayList<Namespace>();
        }

        public String lookup(String fullName) {
            //good for short name
            LookupResult result = context.lookup(fullName);
            if (result != null) {
                return result.getScope().getPathTo(fullName, ClojureParseTreeListener.PATH_SEPARATOR);
            }
            for (Namespace usedNs : usedNamespaces) {
                String res = usedNs.lookup(fullName);
                if (res != null) {
                    return res;
                }
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Namespace namespace = (Namespace) o;
            return name.equals(namespace.name);

        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    private static final String NAMESPACE_SEPARATOR = "/";

    private Namespace currentNamespace;

    private Map<String, Namespace> allNamespaces = new HashMap<>();

    public NamespaceContextResolver(String defaultNsName) {
        currentNamespace = new Namespace(defaultNsName);
        allNamespaces.put(defaultNsName, currentNamespace);
    }

    public Context<Boolean> context() {
        return currentNamespace.context;
    }

    public void enterNamespace(String nsName) {
        allNamespaces.put(currentNamespace.name, currentNamespace);

        //getting previously saved context for namespace or creating new one
        Namespace savedNamespace = allNamespaces.get(nsName);
        if (savedNamespace == null) {
            currentNamespace = new Namespace(nsName);
        } else {
            currentNamespace = savedNamespace;
        }
    }

    public String lookup(ClojureParser.SymbolContext ctx) {

        String fullName = ctx.getText();

        //qualified symbol using namespace lookup
        if (ctx.ns_symbol() != null) {
            String[] parts = fullName.split(NAMESPACE_SEPARATOR);
            String nsName = parts[0];
            String varName = parts[1];

            Namespace savedNamespace = allNamespaces.get(nsName);
            if (savedNamespace != null) {
                return savedNamespace.lookup(varName);
            } else {
                return currentNamespace.lookup(fullName);
            }
        }

        //simple symbol without namespace lookup
        return currentNamespace.lookup(fullName);
    }

}
