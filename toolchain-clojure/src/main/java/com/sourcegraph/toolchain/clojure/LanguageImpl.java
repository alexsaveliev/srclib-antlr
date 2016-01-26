package com.sourcegraph.toolchain.clojure;

import com.sourcegraph.toolchain.clojure.antlr4.ClojureLexer;
import com.sourcegraph.toolchain.clojure.antlr4.ClojureParser;
import com.sourcegraph.toolchain.core.PathUtil;
import com.sourcegraph.toolchain.core.objects.DefKey;
import com.sourcegraph.toolchain.language.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LanguageImpl extends LanguageBase {

    TypeInfos<Scope, String> infos = new TypeInfos<>();

    boolean firstPass = true;

    private Map<File, ParseTree> trees = new HashMap<>();

    public LanguageImpl() {
        super();
        infos.getRoot().setData(new Scope(StringUtils.EMPTY, StringUtils.EMPTY));
    }

    @Override
    public void graph() {
        // first pass to extract defs
        super.graph();

        // second pass to extract refs
        firstPass = false;
        for (Map.Entry<File, ParseTree> entry : trees.entrySet()) {
            processingPath.push(PathUtil.relativizeCwd(entry.getKey().toPath()));
            LOGGER.info("Extracting refs from {}", getCurrentFile());
            try {
                ParseTreeWalker walker = new ParseTreeWalker();
                walker.walk(new ClojureParseTreeListener(this), entry.getValue());
            } catch (Exception e) {
                LOGGER.error("Failed to process {} - unexpected error", getCurrentFile(), e);
            } finally {
                processingPath.pop();
            }
        }
    }

    @Override
    protected void parse(File sourceFile) throws ParseException {
        try {
            GrammarConfiguration configuration = LanguageBase.createGrammarConfiguration(
                    this,
                    sourceFile, ClojureLexer.class,
                    ClojureParser.class,
                    new DefaultErrorListener(sourceFile));
            ParseTree tree = ((ClojureParser) configuration.parser).file();
            ParseTreeWalker walker = new ParseTreeWalker();
            walker.walk(new ClojureParseTreeListener(this), tree);
            trees.put(sourceFile, tree);
        } catch (Exception e) {
            throw new ParseException(e);
        }
    }

    @Override
    protected FileCollector getFileCollector(File rootDir, String repoUri) {
        return new ExtensionBasedFileCollector().extension(".clj");
    }

    @Override
    public String getName() {
        return "clojure";
    }

    @Override
    public DefKey resolve(DefKey source) {
        return null;
    }

    @Override
    public CharStream getCharStream(File sourceFile) throws IOException {
        return super.getCharStream(sourceFile);
    }
}
