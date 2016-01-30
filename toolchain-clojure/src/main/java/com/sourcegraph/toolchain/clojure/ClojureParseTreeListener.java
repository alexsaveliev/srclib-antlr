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


class ClojureParseTreeListener extends ClojureBaseListener {


    private static final Logger LOGGER = LoggerFactory.getLogger(ClojureParseTreeListener.class);

    public static final char PATH_SEPARATOR = '.';

    private LanguageImpl support;

    private NamespaceContextResolver nsContextResolver =  NamespaceContextResolver.getInstance();

    private Map<ParserRuleContext, Boolean> defs = new IdentityHashMap<>();

    ClojureParseTreeListener(LanguageImpl support) {
        this.support = support;
    }

    @Override public void enterSimple_fn(ClojureParser.Simple_fnContext ctx) {
        ClojureParser.Fn_nameContext nameCtx = ctx.fn_name();
        String fnStartKeyword = ctx.fn_start().getText();

        Def fnDef = support.def(nameCtx, DefKind.FUNC);
        fnDef.format(fnStartKeyword, StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
        fnDef.defData.setKind(fnStartKeyword);

        emit(fnDef, nsContextResolver.context().currentScope().getPathTo(fnDef.name, PATH_SEPARATOR));

        nsContextResolver.context().currentScope().put(fnDef.name, true);
        defs.put(ctx.fn_name().symbol(), true);

        nsContextResolver.context().enterScope(new Scope<>(nameCtx.getText(), nsContextResolver.context().currentScope().getPrefix()));

        List<ClojureParser.ParameterContext> params = ctx.arguments().parameter();
        for (ClojureParser.ParameterContext param : params) {
            Def paramDef = support.def(param, DefKind.PARAM);
            paramDef.format("param", StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
            paramDef.defData.setKind("param");

            emit(paramDef, nsContextResolver.context().currentScope().getPathTo(paramDef.name, PATH_SEPARATOR));

            nsContextResolver.context().currentScope().put(param.parameter_name().getText(), true);
            defs.put(param.parameter_name().symbol(), true);
        }

    }

    @Override public void exitSimple_fn(ClojureParser.Simple_fnContext ctx) {
        nsContextResolver.context().exitScope();
    }

    @Override public void enterMulti_fn_def(ClojureParser.Multi_fn_defContext ctx) {

    }

    @Override public void exitMulti_fn_def(ClojureParser.Multi_fn_defContext ctx) {
        nsContextResolver.context().exitScope();
    }

//    @Override
//    public void enterFunction_def(ClojureParser.Function_defContext ctx) {
//        ClojureParser.Fn_nameContext nameCtx = ctx.fn_name();
//        String fnStartKeyword = ctx.fn_start().getText();
//
//        Def fnDef = support.def(nameCtx, DefKind.FUNC);
//        fnDef.format(fnStartKeyword, StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
//        fnDef.defData.setKind(fnStartKeyword);
//
//        emit(fnDef, nsContextResolver.context().currentScope().getPathTo(fnDef.name, PATH_SEPARATOR));
//
//        nsContextResolver.context().currentScope().put(fnDef.name, true);
//        defs.put(ctx.fn_name().symbol(), true);
//
//        nsContextResolver.context().enterScope(new Scope<>(nameCtx.getText(), nsContextResolver.context().currentScope().getPrefix()));
//
//        List<ClojureParser.ParameterContext> params = ctx.arguments().
//        for (ClojureParser.ParameterContext param : params) {
//            Def paramDef = support.def(param, DefKind.PARAM);
//            paramDef.format("param", StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
//            paramDef.defData.setKind("param");
//
//            emit(paramDef, nsContextResolver.context().currentScope().getPathTo(paramDef.name, PATH_SEPARATOR));
//
//            nsContextResolver.context().currentScope().put(param.parameter_name().getText(), true);
//            defs.put(param.parameter_name().symbol(), true);
//        }
//    }
//
//    @Override
//    public void exitFunction_def(ClojureParser.Function_defContext ctx) {
//        nsContextResolver.context().exitScope();
//    }
//
//    @Override
//    public void enterVar_def(ClojureParser.Var_defContext ctx) {
//        ClojureParser.Var_nameContext nameCtx = ctx.var_name();
//        String varStartKeyword = ctx.var_start().getText();
//
//        Def varDef = support.def(nameCtx, DefKind.VAR);
//        varDef.format(varStartKeyword, StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
//        varDef.defData.setKind(varStartKeyword);
//
//        emit(varDef, nsContextResolver.context().currentScope().getPathTo(varDef.name, PATH_SEPARATOR));
//
//        nsContextResolver.context().currentScope().put(varDef.name, true);
//        defs.put(ctx.var_name().symbol(), true);
//    }
//
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
//
//    @Override
//    public void enterIn_ns_def(ClojureParser.In_ns_defContext ctx) {
//        nsContextResolver.enterNamespace(ctx.ns_name().getText());
//    }
//
//    @Override
//    public void enterNs_def(ClojureParser.Ns_defContext ctx) {
//        String nsName = ctx.ns_name().getText();
//
//        nsContextResolver.enterNamespace(nsName);
//
//        List<ClojureParser.ReferenceContext> refs = ctx.references().reference();
//        for (ClojureParser.ReferenceContext ref : refs) {
//            if (ref.use_reference() != null) {
//                List<ClojureParser.Ref_entityContext> refEnts = ref.use_reference().ref_entities().ref_entity();
//                for (ClojureParser.Ref_entityContext refEnt : refEnts) {
//                    if (refEnt.symbol() != null) {
//                        nsContextResolver.addUsedNamespace(refEnt.getText());
//                    } else {
//                        LOGGER.warn("UNSUPPORTED entity = " + refEnt.getText() + "IN NS :USE REFERENCE");
//                    }
//                }
//            } else if (ref.require_reference() != null) {
//                LOGGER.warn(":REQUIRE REFERENCE = " + ref.getText() + "NOT SUPPORTED IN NS = " + nsName);
//
//            } else if (ref.import_reference() != null) {
//                LOGGER.warn(":IMPORT REFERENCE = " + ref.getText() + "NOT SUPPORTED IN NS = " + nsName);
//
//            } else if (ref.other_reference() != null) {
//                LOGGER.warn("REFERENCE = " + ref.getText() + "NOT SUPPORTED IN NS = " + nsName);
//            }
//        }
//    }
//
//    @Override
//    public void enterLet_form(ClojureParser.Let_formContext ctx) {
//        nsContextResolver.context().enterScope(nsContextResolver.context().currentScope().next(PATH_SEPARATOR));
//
//        List<ClojureParser.BindingContext> bindingsCtx = ctx.bindings().binding();
//        for (ClojureParser.BindingContext bindingCtx : bindingsCtx) {
//            if (bindingCtx.var_name() != null) {
//
//                Def letvarDef = support.def(bindingCtx.var_name(), DefKind.LETVAR);
//                letvarDef.format("letvar", StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
//                letvarDef.defData.setKind("letvar");
//
//                emit(letvarDef, nsContextResolver.context().currentScope().getPathTo(letvarDef.name, PATH_SEPARATOR));
//
//                nsContextResolver.context().currentScope().put(bindingCtx.var_name().getText(), true);
//                defs.put(bindingCtx.var_name().symbol(), true);
//            } else {
//                LOGGER.warn("UNSUPPORTED BINDING FORM = " + bindingCtx.getText() + "FOR LET DEFINITION = " + ctx.getText() + " WAS FOUND");
//            }
//        }
//    }
//
//    @Override
//    public void exitLet_form(ClojureParser.Let_formContext ctx) {
//        nsContextResolver.context().exitScope();
//    }

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