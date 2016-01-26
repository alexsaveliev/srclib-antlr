package com.sourcegraph.toolchain.clojure;

import com.sourcegraph.toolchain.clojure.antlr4.ClojureLexer;
import com.sourcegraph.toolchain.clojure.antlr4.ClojureParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.BitSet;

public class scan {

    static int total = 0;
    static int errors = 0;


    public static void main(String args[]) throws Exception {
        //long startTime = System.currentTimeMillis();
        String root = ".";
        if (args.length > 0) {
            root = args[0];
        }
        Path p = Paths.get(root);
        Path path = Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                File f = file.toFile();
                if (!f.getName().endsWith(".clj")
                        && !f.getName().endsWith(".cljs")
                        && !f.getName().endsWith(".edn")
                        && !f.getName().endsWith(".cljc")
                        ) {
                    return FileVisitResult.CONTINUE;
                }

                total++;
                ErrorListenerImpl listener = new ErrorListenerImpl(f);
                System.out.println("Processing " + file);
                try {
                    LanguageImpl support = new LanguageImpl();
                    CharStream stream = support.getCharStream(f);
                    ClojureLexer lexer = new ClojureLexer(stream);
                    lexer.removeErrorListeners();
                    lexer.addErrorListener(listener);

                    TokenStream tokens = new CommonTokenStream(lexer);
                    ClojureParser parser = new ClojureParser(tokens);
                    parser.removeErrorListeners();
                    parser.addErrorListener(listener);

                    ClojureParser.FileContext ctx = parser.file();

                    //print tokens
                    //lex.printTokens(file.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    return FileVisitResult.TERMINATE;
                }
                if (listener.hasErrors) {
                    errors++;
                }
                return listener.hasErrors ? FileVisitResult.CONTINUE : FileVisitResult.CONTINUE;
            }
        });
        //long stopTime = System.currentTimeMillis();
        //long elapsedTime = stopTime - startTime;
        //System.out.println("Time in mls = " + elapsedTime);
        System.out.println("TOTAL= " + total);
        System.out.println("ERRORS = " + errors);
    }

    static class ErrorListenerImpl implements ANTLRErrorListener {

        private static final Logger LOGGER = LoggerFactory.getLogger(ErrorListenerImpl.class);

        private File sourceFile;
        boolean hasErrors;

        public ErrorListenerImpl(File sourceFile) {
            this.sourceFile = sourceFile;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            LOGGER.warn("{} at {}:{}: {}", this.sourceFile, line, charPositionInLine, msg);
            hasErrors = true;
        }

        @Override
        public void reportAmbiguity(Parser parser,
                                    DFA dfa,
                                    int i,
                                    int i1,
                                    boolean b,
                                    BitSet bitSet,
                                    ATNConfigSet atnConfigSet) {

        }

        @Override
        public void reportAttemptingFullContext(Parser parser,
                                                DFA dfa,
                                                int i,
                                                int i1,
                                                BitSet bitSet,
                                                ATNConfigSet atnConfigSet) {

        }

        @Override
        public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet) {
        }

    }
}