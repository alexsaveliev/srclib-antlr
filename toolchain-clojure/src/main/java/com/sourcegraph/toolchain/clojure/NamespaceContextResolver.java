package com.sourcegraph.toolchain.clojure;

import com.sourcegraph.toolchain.clojure.antlr4.ClojureParser;
import com.sourcegraph.toolchain.language.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This class consists methods and variables which are used for saving namespaces with their contexts,
 * getting namespace by name, name resolution in all known namespaces
 */
public class NamespaceContextResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceContextResolver.class);

    /**
     * Delimiter for namespace and name separation in Clojure, for example ns1/user1
     */
    public static final String NAMESPACE_SEPARATOR = "/";

    /**
     * Default namespace in Clojure
     */
    private static final String CLOJURE_DEFAULT_NAMESPACE_NAME = "user";

    private Namespace currentNamespace;

    /**
     * List of all user-defined namespaces for current program
     */
    private Map<String, Namespace> allNamespaces = new HashMap<>();

    public NamespaceContextResolver() {
        currentNamespace = new Namespace(CLOJURE_DEFAULT_NAMESPACE_NAME, this);
        allNamespaces.put(CLOJURE_DEFAULT_NAMESPACE_NAME, currentNamespace);
    }

    public Namespace getNamespaceByName(String name) {
        return allNamespaces.get(name);
    }

    public Context<Boolean> context() {
        return currentNamespace.getContext();
    }

    /**
     * Saves information about current namespace - name, context when program enter new one
     * Creates new namespace with defined name
     * Changes current namespace
     * @param nsName Name of new namespace
     */
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

    /**
     * Provides name resolution functionality
     * First tries to qualify given identifier as simple or full-qualified - with namespace prefix
     * If name is full qualified tries to split it into namespace name and simple name, than
     * looks for specified namespace, if such namespace is found, tries to resolve simple name inside it
     * @param ctx Identifier to resolve
     * @return Result of resolution - full path of found identifier
     */
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

    /**
     * @return Current namespace name
     */
    public String currentNamespaceName() {
        return currentNamespace.getName();
    }

    /**
     * Adds namespace name to list of used by current namespace
     * @param namespace Namespace name
     */
    public void addUsedNamespace(String namespace) {
        currentNamespace.addUsedNamespace(namespace);
    }

}
