package com.sourcegraph.toolchain.clojure;

import com.sourcegraph.toolchain.language.Context;
import com.sourcegraph.toolchain.language.LookupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Namespace {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceContextResolver.class);

    private String name;
    private Context<Boolean> context;
    private List<String> usedNsNames;

    //private List<String> usedDefs;
    //private List<String> excludedDefs;

//    private NamespaceContextResolver namespaceContextResolver = NamespaceContextResolver.getInstance();

    private NamespaceContextResolver namespaceContextResolver;

    public Namespace(String name, NamespaceContextResolver namespaceContextResolver) {
        this.name = name;
        context = new Context<>();
        usedNsNames = new ArrayList<>();
        this.namespaceContextResolver = namespaceContextResolver;
    }

    public String lookup(String fullName) {
        //good for short name
        LookupResult result = context.lookup(fullName);
        if (result != null) {
            String namePath = result.getScope().getPathTo(fullName, ClojureParseTreeListener.PATH_SEPARATOR);
            return name + namespaceContextResolver.NAMESPACE_SEPARATOR + namePath;
        }
        for (String usedNs : usedNsNames) {
            Namespace namespace = namespaceContextResolver.getNamespaceByName(usedNs);
            if (namespace == null) {
               // LOGGER.warn("UNABLE TO ACCESS TO DEF OF NAMESPACE {}", usedNs);
                return null;
            }
            String res = namespace.lookup(fullName);
            if (res != null) {
                return res;
            }
        }
        //LOGGER.warn("NAME = {} WAS NOT FOUND IN NS: {}", fullName, name);
        return null;
    }

    public String getName() {
        return name;
    }

    public Context<Boolean> getContext() {
        return context;
    }

    public void addUsedNamespace(String namespace) {
        usedNsNames.add(namespace);
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
