package frontend.parser.statement;

import frontend.lexer.TokenStream;
import frontend.parser.declaration.Decl;
import frontend.type.TokenType;

public interface BlockItem {
    // BlockItem → Decl | Stmt
    //      Decl → ConstDecl | VarDecl
    // ConstDecl → $'const'$ BType ConstDef { ',' ConstDef } ';'
    //   VarDecl → BType VarDef { ',' VarDef } ';'
    //     BType → $'int'$ | $'char'$
    static BlockItem parse(TokenStream stream) {
        if (stream.isNow(TokenType.CONSTTK, TokenType.INTTK, TokenType.CHARTK)) {
            return Decl.parse(stream);
        } else {
            return Stmt.parse(stream);
        }
    }
}
