package com.sourcegraph.toolchain.clojure;

import com.sourcegraph.toolchain.language.Context;
import com.sourcegraph.toolchain.language.LookupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents Namespace entity with its name, context and list of used namespaces
 */
public class Namespace {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceContextResolver.class);

    /**
     * Namespace name, sets once during creation
     */
    private String name;

    /**
     * Namespace context for lookup of definitions
     */
    private Context<Boolean> context;

    /**
     * List of used namespaces
     * For example (:use 'ns1 'ns2) - ns1, ns2 will be in the list
     */
    private List<String> usedNsNames;

    /**
     * General context resolver, saves all namespaces, provides namespace by its name
     */
    private NamespaceContextResolver namespaceContextResolver;

    public Namespace(String name, NamespaceContextResolver namespaceContextResolver) {
        this.name = name;
        context = new Context<>();
        usedNsNames = new ArrayList<>();
        this.namespaceContextResolver = namespaceContextResolver;
    }

    /**
     * Lookup method for names, tries to find specified names in the current namespace (context).
     * If such lookup does not succeed tries to find name in the used namespaces
     * @param fullName Full name of identifier for lookup
     * @return Full path of found identifier
     */
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

    /**
     * Saves name of namespace in the list of used namespaces
     * @param namespace Name of used namespace
     */
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
