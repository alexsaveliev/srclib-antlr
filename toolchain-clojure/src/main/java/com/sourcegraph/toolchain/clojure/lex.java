package com.sourcegraph.toolchain.clojure;

import com.sourcegraph.toolchain.clojure.antlr4.ClojureLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

import java.io.File;
import java.io.IOException;

public class lex {

    public static void main(String args[]) throws Exception {
        String root = ".";
        if (args.length > 0) {
            root = args[0];
        }

        printTokens(root);
    }

    public static void printTokens(String filePath) {
        LanguageImpl support = new LanguageImpl();
        CharStream stream = null;
        try {
            stream = support.getCharStream(new File(filePath));
            ClojureLexer lexer = new ClojureLexer(stream);
            TokenStream tokens = new CommonTokenStream(lexer);
            Token t = lexer.nextToken();
            while (t.getType() != Token.EOF) {
                System.out.println(t);
                t = lexer.nextToken();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}