package util.dump;

import config.Config;
import frontend.lexer.Token;
import frontend.type.TokenType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class DumpTokenList {
    public static void dump(ArrayList<Token> tokens) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(Config.dumpTokenListFileName));
        for (Token token : tokens) {
            if (token.type() != TokenType.EOF) {
                out.write(token.type().toString() + " " + token.strVal() + "\n");
            }
        }
        out.close();
    }
}
