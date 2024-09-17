package frontend.parser.declaration;

import global.Config;
import frontend.lexer.TokenStream;
import frontend.parser.declaration.constant.ConstDecl;
import frontend.parser.declaration.variable.VarDecl;
import frontend.parser.statement.BlockItem;
import frontend.type.TokenType;

public interface Decl extends BlockItem {
    // Decl → ConstDecl | VarDecl
    static Decl parse(TokenStream stream) {
        // ConstDecl → $'const'$ BType ConstDef { ',' ConstDef } ';'
        // VarDecl → BType VarDef { ',' VarDef } ';'
        //   BType → $'int'$ | $'char'$
        if (stream.isNow(TokenType.CONSTTK)) {
            return new ConstDecl(stream);
        } else if (stream.isNow(TokenType.INTTK, TokenType.CHARTK)) {
            return new VarDecl(stream);
        } else {
            if (Config.parserThrowable) {
                throw new RuntimeException("When Decl.parse(), unexpected token: " + stream.getNow());
            } else {
                return null;
            }
        }
    }
}
