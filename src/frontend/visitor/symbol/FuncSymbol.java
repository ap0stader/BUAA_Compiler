package frontend.visitor.symbol;

import IR.type.Type;
import frontend.lexer.Token;

public class FuncSymbol extends Symbol {
    public FuncSymbol(Type type, Token ident) {
        super(type, ident.strVal(), ident.line());
    }
}
