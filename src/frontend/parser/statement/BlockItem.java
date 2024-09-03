package frontend.parser.statement;

import frontend.lexer.TokenStream;

public interface BlockItem {
    // BlockItem → Decl | Stmt
    static BlockItem parse(TokenStream stream) {
        return null;
    }
}
