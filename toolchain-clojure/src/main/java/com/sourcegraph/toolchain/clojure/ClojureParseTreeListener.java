package com.sourcegraph.toolchain.clojure;

import com.sourcegraph.toolchain.clojure.antlr4.ClojureBaseListener;
import com.sourcegraph.toolchain.clojure.antlr4.ClojureParser;
import com.sourcegraph.toolchain.core.objects.Def;
import com.sourcegraph.toolchain.core.objects.DefData;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.core.objects.Ref;
import com.sourcegraph.toolchain.language.Context;
import com.sourcegraph.toolchain.language.LookupResult;
import com.sourcegraph.toolchain.language.Scope;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ClojureParseTreeListener extends ClojureBaseListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClojureParseTreeListener.class);
    private static final char PATH_SEPARATOR = '.';

    private LanguageImpl support;

    Context<Boolean> context = new Context<>();

    //private boolean isInFunctionDef;


    ClojureParseTreeListener(LanguageImpl support) {
        this.support = support;
    }

    @Override
    public void enterFunction_def(ClojureParser.Function_defContext ctx) {
        ClojureParser.Fn_nameContext nameCtx = ctx.fn_name();
        context.enterScope(new Scope<Boolean>(nameCtx.getText())); // params here
    }

    @Override
    public void exitFunction_def(ClojureParser.Function_defContext ctx) {
        context.exitScope();

        ClojureParser.Fn_nameContext nameCtx = ctx.fn_name();
        String fnStartKeyword = ctx.fn_start().getText();

        Def fnDef = support.def(nameCtx, DefKind.FUNC);
        fnDef.format(fnStartKeyword, StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
        fnDef.defData.setKind(fnStartKeyword);

        emit(fnDef, context.currentScope().getPathTo(fnDef.name, PATH_SEPARATOR));

        context.currentScope().put(fnDef.name, true);

    }

    @Override
    public void enterVar_def(ClojureParser.Var_defContext ctx) {
//        ClojureParser.Var_nameContext nameCtx = ctx.var_name();
//        String varStartKeyword = ctx.var_start().getText();
//
//        Def varDef = support.def(nameCtx, DefKind.VAR);
//        varDef.format(varStartKeyword, StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
//        varDef.defData.setKind(varStartKeyword);
//
//        emit(varDef, context.currentScope().getPathTo(varDef.name, PATH_SEPARATOR));
//
//        context.currentScope().put(varDef.name, true);
    }

    @Override
    public void exitVar_def(ClojureParser.Var_defContext ctx) {
        ClojureParser.Var_nameContext nameCtx = ctx.var_name();
        String varStartKeyword = ctx.var_start().getText();

        Def varDef = support.def(nameCtx, DefKind.VAR);
        varDef.format(varStartKeyword, StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
        varDef.defData.setKind(varStartKeyword);

        emit(varDef, context.currentScope().getPathTo(varDef.name, PATH_SEPARATOR));

        context.currentScope().put(varDef.name, true);
    }


    @Override
    public void enterSymbol(ClojureParser.SymbolContext ctx) {
        String ident = ctx.getText();
        LookupResult result = context.lookup(ident);
        if (result == null) {
            return;
        }

        Ref ref = support.ref(ctx);
        emit(ref, result.getScope().getPathTo(ident, PATH_SEPARATOR));
    }

//    @Override
//    public void enterList(ClojureParser.ListContext ctx) {
//        List<FormContext> listForms = ctx.forms().form();
//        FormContext first = listForms.get(0);
//        LOGGER.debug("Number of pass = " + String.valueOf((support.firstPass == true) ? 1 : 2));
//        LOGGER.debug("Entered list with such first element = " + first.getText());
//
//        //check if we found call of function here
//        String ident = first.getText();
//        LookupResult result = context.lookup(ident);
//        if (result == null) {
//            return;
//        }
//
//        Ref ref = support.ref(first);
//        emit(ref, result.getScope().getPathTo(ident, PATH_SEPARATOR));
//    }
//
//    @Override
//    public void exitList(ClojureParser.ListContext ctx) {
//    }

    private void emit(Def def, String path) {
        if (!support.firstPass) {
            return;
        }
        def.defKey = new DefKey(null, path);
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

