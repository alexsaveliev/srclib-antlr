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
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


class ClojureParseTreeListener extends ClojureBaseListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClojureParseTreeListener.class);
    private static final char PATH_SEPARATOR = '.';

    private LanguageImpl support;

    Context<Boolean> context = new Context<>();

    private Map<ParserRuleContext, Boolean> defs = new IdentityHashMap<>();

    ClojureParseTreeListener(LanguageImpl support) {
        this.support = support;
    }

    @Override
    public void enterFunction_def(ClojureParser.Function_defContext ctx) {
        ClojureParser.Fn_nameContext nameCtx = ctx.fn_name();
        String fnStartKeyword = ctx.fn_start().getText();

        Def fnDef = support.def(nameCtx, DefKind.FUNC);
        fnDef.format(fnStartKeyword, StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
        fnDef.defData.setKind(fnStartKeyword);

        emit(fnDef, context.currentScope().getPathTo(fnDef.name, PATH_SEPARATOR));

        context.currentScope().put(fnDef.name, true);
        defs.put(ctx.fn_name().symbol(), true);

        context.enterScope(new Scope<>(nameCtx.getText(), context.currentScope().getPrefix()));

        List<ClojureParser.ParameterContext> params = ctx.arguments().parameter();
        for (ClojureParser.ParameterContext param : params) {
            Def paramDef = support.def(param, DefKind.PARAM);
            paramDef.format("param", StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
            paramDef.defData.setKind("param");

            emit(paramDef, context.currentScope().getPathTo(paramDef.name, PATH_SEPARATOR));

            context.currentScope().put(param.parameter_name().getText(), true);
            defs.put(param.parameter_name().symbol(), true);
        }
    }

    @Override
    public void exitFunction_def(ClojureParser.Function_defContext ctx) {
        context.exitScope();
    }

    @Override
    public void enterVar_def(ClojureParser.Var_defContext ctx) {
        ClojureParser.Var_nameContext nameCtx = ctx.var_name();
        String varStartKeyword = ctx.var_start().getText();

        Def varDef = support.def(nameCtx, DefKind.VAR);
        varDef.format(varStartKeyword, StringUtils.EMPTY, DefData.SEPARATOR_EMPTY);
        varDef.defData.setKind(varStartKeyword);

        emit(varDef, context.currentScope().getPathTo(varDef.name, PATH_SEPARATOR));

        context.currentScope().put(varDef.name, true);
        defs.put(ctx.var_name().symbol(), true);
    }

    @Override
    public void enterSymbol(ClojureParser.SymbolContext ctx) {
        //check if it's name of some declaration
        if (defs.containsKey(ctx)) {
            return;
        }

        String ident = ctx.getText();
        LookupResult result = context.lookup(ident);
        if (result == null) {
            return;
        }

        Ref ref = support.ref(ctx);
        emit(ref, result.getScope().getPathTo(ident, PATH_SEPARATOR));
    }

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

