package com.sourcegraph.toolchain.clojure;

import com.sourcegraph.toolchain.clojure.antlr4.ClojureBaseListener;
import com.sourcegraph.toolchain.clojure.antlr4.ClojureParser;
import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefData;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.core.objects.Ref;
import com.sourcegraph.toolchain.language.Scope;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import java.util.StringJoiner;


class ClojureParseTreeListener extends ClojureBaseListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClojureParseTreeListener.class);

    public static final char PATH_SEPARATOR = '.';

    private LanguageImpl support;

    private NamespaceContextResolver nsContextResolver = new NamespaceContextResolver();

    private Map<ParserRuleContext, Boolean> defs = new IdentityHashMap<>();

    ClojureParseTreeListener(LanguageImpl support) {
        this.support = support;
    }

    @Override
    public void enterFn_binding(ClojureParser.Fn_bindingContext ctx) {
        nsContextResolver.context().enterScope(nsContextResolver.context().currentScope().next(PATH_SEPARATOR));
        List<ClojureParser.ParameterContext> params = ctx.arguments().parameter();
        saveParametersInScope(params);
    }

    @Override
    public void exitFn_binding(ClojureParser.Fn_bindingContext ctx) {
        nsContextResolver.context().exitScope();
    }

    @Override
    public void enterAnonym_fn_binding(ClojureParser.Anonym_fn_bindingContext ctx) {
        nsContextResolver.context().enterScope(nsContextResolver.context().currentScope().next(PATH_SEPARATOR));
        List<ClojureParser.ParameterContext> params = ctx.arguments().parameter();
        saveParametersInScope(params);
    }

    @Override
    public void exitAnonym_fn_binding(ClojureParser.Anonym_fn_bindingContext ctx) {
        nsContextResolver.context().exitScope();
    }

    @Override
    public void enterLetfn_function_binding(ClojureParser.Letfn_function_bindingContext ctx) {
        nsContextResolver.context().enterScope(nsContextResolver.context().currentScope().next(PATH_SEPARATOR));
        List<ClojureParser.ParameterContext> params = ctx.arguments().parameter();
        saveParametersInScope(params);
    }

    @Override
    public void exitLetfn_function_binding(ClojureParser.Letfn_function_bindingContext ctx) {
        nsContextResolver.context().exitScope();
    }

    @Override
    public void enterSimple_fn_def(ClojureParser.Simple_fn_defContext ctx) {
        ClojureParser.Fn_nameContext nameCtx = ctx.fn_name();
        String fnStartKeyword = ctx.fn_start().getText();

        List<ClojureParser.ParameterContext> params = ctx.arguments().parameter();
        ClojureParser.Last_argumentsContext lastArgsCtx = ctx.last_arguments();

        enterFunctionWithName(nameCtx, fnStartKeyword, formParamsDoc(params, lastArgsCtx));
        saveParametersInScope(params);

        //Processing of last arguments
        if (lastArgsCtx != null) {
            List<ClojureParser.ParameterContext> lastArgs = lastArgsCtx.parameters().parameter();
            saveParametersInScope(lastArgs);
        }
    }

    @Override
    public void exitSimple_fn_def(ClojureParser.Simple_fn_defContext ctx) {
        nsContextResolver.context().exitScope();
    }

    @Override
    public void enterMulti_fn_def(ClojureParser.Multi_fn_defContext ctx) {
        ClojureParser.Fn_nameContext nameCtx = ctx.fn_name();
        String fnStartKeyword = ctx.fn_start().getText();

        enterFunctionWithName(nameCtx, fnStartKeyword);
    }

    @Override
    public void exitMulti_fn_def(ClojureParser.Multi_fn_defContext ctx) {
        nsContextResolver.context().exitScope();
    }

    @Override
    public void enterUndefined_fn_with_name(ClojureParser.Undefined_fn_with_nameContext ctx) {
        ClojureParser.Fn_nameContext nameCtx = ctx.fn_name();
        String fnStartKeyword = ctx.fn_start().getText();

        enterFunctionWithName(nameCtx, fnStartKeyword);
        LOGGER.warn("FUNCTION {} WITH UNDEFINED BODY OR PARAMETERS WAS FOUND, unable to process it fully", ctx.getText());
    }

    @Override
    public void exitUndefined_fn_with_name(ClojureParser.Undefined_fn_with_nameContext ctx) {
        nsContextResolver.context().exitScope();
    }

    @Override
    public void enterUndefined_fn(ClojureParser.Undefined_fnContext ctx) {
        LOGGER.warn("UNDEFINED FUNCTION {} WAS FOUND, unable to process it", ctx.getText());
    }

    @Override
    public void enterSimple_anonym_fn_def(ClojureParser.Simple_anonym_fn_defContext ctx) {
        enterAnonymousFunction(ctx.fn_name());

        List<ClojureParser.ParameterContext> params = ctx.arguments().parameter();
        saveParametersInScope(params);

        ClojureParser.Last_argumentsContext lastArgsCtx = ctx.last_arguments();
        //Processing last arguments
        if (lastArgsCtx != null) {
            List<ClojureParser.ParameterContext> lastArgs = lastArgsCtx.parameters().parameter();
            saveParametersInScope(lastArgs);
        }
    }

    @Override
    public void exitSimple_anonym_fn_def(ClojureParser.Simple_anonym_fn_defContext ctx) {
        nsContextResolver.context().exitScope();
    }

    @Override
    public void enterMulti_anonym_fn_def(ClojureParser.Multi_anonym_fn_defContext ctx) {
        enterAnonymousFunction(ctx.fn_name());
    }

    @Override
    public void exitMulti_anonym_fn_def(ClojureParser.Multi_anonym_fn_defContext ctx) {
        nsContextResolver.context().exitScope();
    }

    @Override
    public void enterUndefined_anonym_fn(ClojureParser.Undefined_anonym_fnContext ctx) {
        LOGGER.warn("UNDEFINED ANONYM FUNCTION FN {} WAS FOUND, unable to process it", ctx.getText());
    }

    @Override
    public void enterSimple_letfn_form(ClojureParser.Simple_letfn_formContext ctx) {
        nsContextResolver.context().enterScope(nsContextResolver.context().currentScope().next(PATH_SEPARATOR));
    }

    @Override
    public void exitSimple_letfn_form(ClojureParser.Simple_letfn_formContext ctx) {
        nsContextResolver.context().exitScope();
    }

    @Override
    public void enterUndefined_letfn_form(ClojureParser.Undefined_letfn_formContext ctx) {
        LOGGER.warn("UNDEFINED LETFN FORM {} WAS FOUND, unable to process it", ctx.getText());
    }

    @Override
    public void enterSimple_letfn_function_def(ClojureParser.Simple_letfn_function_defContext ctx) {
        ClojureParser.Fn_nameContext nameCtx = ctx.fn_name();
        List<ClojureParser.ParameterContext> params = ctx.arguments().parameter();

        enterFunctionWithName(nameCtx, "letfn", formParamsDoc(params, null));
        saveParametersInScope(params);
    }

    @Override
    public void exitSimple_letfn_function_def(ClojureParser.Simple_letfn_function_defContext ctx) {
        nsContextResolver.context().exitScope();
    }

    @Override
    public void enterMultiple_letfn_function_def(ClojureParser.Multiple_letfn_function_defContext ctx) {
        ClojureParser.Fn_nameContext nameCtx = ctx.fn_name();
        enterFunctionWithName(nameCtx, "letfn");
    }

    @Override
    public void exitMultiple_letfn_function_def(ClojureParser.Multiple_letfn_function_defContext ctx) {
        nsContextResolver.context().exitScope();
    }

    @Override
    public void enterSimple_var_def(ClojureParser.Simple_var_defContext ctx) {
        ClojureParser.Var_nameContext nameCtx = ctx.var_name();
        String varStartKeyword = ctx.var_start().getText();

        Def varDef = support.def(nameCtx, DefKind.VAR);
        varDef.format(varStartKeyword, StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
        varDef.defData.setKind(varStartKeyword);

        Scope currentScope = nsContextResolver.context().currentScope();

        emit(varDef, currentScope.getPathTo(varDef.name, PATH_SEPARATOR));
        currentScope.put(varDef.name, true);
        defs.put(nameCtx.symbol(), true);
    }

    @Override
    public void enterUndefined_var_def(ClojureParser.Undefined_var_defContext ctx) {
        LOGGER.warn("UNDEFINED VAR {} WAS FOUND, unable to process it", ctx.getText());
    }

    @Override
    public void enterSymbol(ClojureParser.SymbolContext ctx) {
        //check if it's name of some declaration
        if (defs.containsKey(ctx)) {
            return;
        }

        String pathRes = nsContextResolver.lookup(ctx);
        if (pathRes == null) {
            return;
        }

        Ref ref = support.ref(ctx);
        //LOGGER.debug("lookup res = " + result);
        emit(ref, pathRes);
    }

    @Override
    public void enterSimple_in_ns_def(ClojureParser.Simple_in_ns_defContext ctx) {
        nsContextResolver.enterNamespace(ctx.ns_name().getText());
    }

    @Override
    public void enterUndefined_in_ns_def(ClojureParser.Undefined_in_ns_defContext ctx) {
        LOGGER.warn("UNDEFINED NAMESPACE {} WAS FOUND, unable to process it", ctx.getText());
    }

    @Override
    public void enterSimple_ns_def(ClojureParser.Simple_ns_defContext ctx) {
        ClojureParser.Ns_nameContext nsNameCtx = ctx.ns_name();
        String nsName = nsNameCtx.getText();

        nsContextResolver.enterNamespace(nsName);
        defs.put(nsNameCtx.symbol(), true);

        for (ClojureParser.ReferenceContext ref : ctx.references().reference()) {
            String refKeyword = ref.keyword().getText();

            //add support for other keywords
            if (refKeyword.equals(":use")) {
                List<ClojureParser.Ref_entityContext> refEnts = ref.ref_entities().ref_entity();
                for (ClojureParser.Ref_entityContext refEnt : refEnts) {
                    //add support for lists and vectors
                    if (refEnt.symbol() != null) {
                        nsContextResolver.addUsedNamespace(refEnt.getText());
                    } else {
                        LOGGER.warn("UNSUPPORTED entity {} IN NS :USE REFERENCE", refEnt.getText());
                    }
                }
            } else {
                LOGGER.warn("REFERENCE = {} NOT SUPPORTED IN NS = {}", ref.getText(), nsName);
            }
        }
    }

    @Override
    public void enterUndefined_ns_with_name(ClojureParser.Undefined_ns_with_nameContext ctx) {
        ClojureParser.Ns_nameContext nsNameCtx = ctx.ns_name();
        String nsName = nsNameCtx.getText();

        nsContextResolver.enterNamespace(nsName);
        defs.put(nsNameCtx.symbol(), true);

        LOGGER.warn(" NAMESPACE WITH UNSUPPORTED BODY WAS FOUND, unable to process it fully", ctx.getText());
    }

    @Override
    public void enterUndefined_ns_def(ClojureParser.Undefined_ns_defContext ctx) {
        LOGGER.warn("UNDEFINED NAMESPACE {} WAS FOUND, unable to process it", ctx.getText());
    }

    @Override
    public void enterSimple_binding(ClojureParser.Simple_bindingContext ctx) {
        ClojureParser.Var_nameContext varCtx = ctx.var_name();
        Def bindVar = support.def(varCtx, DefKind.BINDING_VAR);
        bindVar.format("binding_var", StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
        bindVar.defData.setKind("binding_var");

        Scope currentScope = nsContextResolver.context().currentScope();
        emit(bindVar, currentScope.getPathTo(bindVar.name, PATH_SEPARATOR));

        currentScope.put(varCtx.getText(), true);
        defs.put(varCtx.symbol(), true);
    }

    @Override
    public void enterUndefined_binding(ClojureParser.Undefined_bindingContext ctx) {
        LOGGER.warn("UNDEFINED BINDING {} WAS FOUND, unable to process it", ctx.getText());
    }

    @Override
    public void enterLet_form(ClojureParser.Let_formContext ctx) {
        nsContextResolver.context().enterScope(nsContextResolver.context().currentScope().next(PATH_SEPARATOR));
    }

    @Override
    public void exitLet_form(ClojureParser.Let_formContext ctx) {
        nsContextResolver.context().exitScope();
    }

    @Override
    public void enterLoop_form(ClojureParser.Loop_formContext ctx) {
        nsContextResolver.context().enterScope(nsContextResolver.context().currentScope().next(PATH_SEPARATOR));
    }

    @Override
    public void exitLoop_form(ClojureParser.Loop_formContext ctx) {
        nsContextResolver.context().exitScope();
    }

    /**
     * Forms docstring with parameters description of function
     * @param params List of parameters for defined function
     * @param lastArgs List of last arguments - optional parameters after '&' sign
     * @return String doc representation for Clojure formatter - "param1 param2 & param3"
     */
    private String formParamsDoc(List<ClojureParser.ParameterContext> params, ClojureParser.Last_argumentsContext lastArgs) {
        StringJoiner joiner = new StringJoiner(" ");
        for (ClojureParser.ParameterContext param : params) {
            joiner.add(param.parameter_name().getText());
        }

        if (lastArgs != null) {
            joiner.add(" & ");
            List<ClojureParser.ParameterContext> lastParams = lastArgs.parameters().parameter();
            for (ClojureParser.ParameterContext param : lastParams) {
                joiner.add(param.parameter_name().getText());
            }
        }
        return joiner.toString();
    }

    private void enterFunctionWithName(ClojureParser.Fn_nameContext nameCtx, String fnStartKeyword) {
        enterFunctionWithName(nameCtx, fnStartKeyword, StringUtils.EMPTY);
    }

    /**
     * Emits new function definition into current context, enter new named scope
     * @param nameCtx
     * @param fnStartKeyword Function start keyword - defn, defmacro, defonce..
     * @param paramDoc String representation of parameters used by Clojure formatter
     */
    private void enterFunctionWithName(ClojureParser.Fn_nameContext nameCtx, String fnStartKeyword, String paramDoc) {
        Def fnDef = support.def(nameCtx, DefKind.FUNC);
        fnDef.format(fnStartKeyword, paramDoc, DefData.SEPARATOR_SPACE);
        fnDef.defData.setKind(fnStartKeyword);

        Scope currentScope = nsContextResolver.context().currentScope();

        emit(fnDef, currentScope.getPathTo(fnDef.name, PATH_SEPARATOR));
        currentScope.put(fnDef.name, true);
        defs.put(nameCtx.symbol(), true);

        String currentScopeName = currentScope.getName();
        String prefix = currentScope.getPrefix();
        //checking if we are inside root context - to avoid adding path separator in this case
        //prefix is modified to resolve situation when named scope is included into unnamed, see letfn tests
        if (!currentScopeName.equals(StringUtils.EMPTY)) {
            prefix = currentScopeName + PATH_SEPARATOR + prefix;
        }
        nsContextResolver.context().enterScope(new Scope<>(nameCtx.getText(), prefix));
    }

    /**
     * Enters function without name, just creates new scope, no definitions are provided
     * Fn_name is optional and useless, so it is skipped here
     * @param fn_name
     */
    private void enterAnonymousFunction(ClojureParser.Fn_nameContext fn_name) {
        nsContextResolver.context().enterScope(nsContextResolver.context().currentScope().next(PATH_SEPARATOR));
    }

    /**
     * Saves parameters, last arguments in scope for simple function definition, anonymous function definition,
     * different function bindings
     * @param params
     */
    private void saveParametersInScope(List<ClojureParser.ParameterContext> params) {
        for (ClojureParser.ParameterContext param : params) {
            ClojureParser.Parameter_nameContext paramNameCtx = param.parameter_name();
            Def paramDef = support.def(paramNameCtx, DefKind.PARAM);
            paramDef.format("param", StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
            paramDef.defData.setKind("param");

            Scope currentScope = nsContextResolver.context().currentScope();

            emit(paramDef, currentScope.getPathTo(paramDef.name, PATH_SEPARATOR));
            currentScope.put(paramNameCtx.getText(), true);
            defs.put(paramNameCtx.symbol(), true);
        }
    }

    private void emit(Def def, String path) {
        if (!support.firstPass) {
            return;
        }
        String pathWithNs = nsContextResolver.currentNamespaceName() + nsContextResolver.NAMESPACE_SEPARATOR + path;
        def.defKey = new DefKey(null, pathWithNs);
        support.emit(def);
    }

    private void emit(Ref ref, String path) {
        if (support.firstPass) {
            return;
        }
        ref.defKey = new DefKey(null, path);
        support.emit(ref);
    }
}