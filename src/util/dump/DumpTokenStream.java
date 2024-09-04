package util.dump;

import config.Config;
import frontend.lexer.Token;
import frontend.type.TokenType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class DumpTokenStream {
    public static void dump(ArrayList<Token> tokens) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(Config.dumpTokenStreamFileName));
        for (Token token : tokens) {
            if (token.type() != TokenType.EOF) {
                if (Config.dumpTokenStreamLineNumber) {
                    out.write(token.type().toString() + " " + token.strVal() + " " + token.line() + "\n");
                } else {
                    out.write(token.type().toString() + " " + token.strVal() + "\n");
                }
            }
        }
        out.close();
    }
}
