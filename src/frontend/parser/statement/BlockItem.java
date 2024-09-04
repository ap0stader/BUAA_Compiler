package frontend.parser.statement;

import frontend.lexer.TokenStream;
import frontend.parser.declaration.Decl;
import frontend.type.TokenType;

public interface BlockItem {
    // BlockItem → Decl | Stmt
    //      Decl → ConstDecl | VarDecl
    // ConstDecl → $'const'$ 'int' ConstDef { ',' ConstDef } ';'
    //   VarDecl → $'int'$ VarDef { ',' VarDef } ';'
    static BlockItem parse(TokenStream stream) {
        if (stream.isNow(TokenType.CONSTTK, TokenType.INTTK)) {
            return Decl.parse(stream);
        } else {
            return Stmt.parse(stream);
        }
    }
}
