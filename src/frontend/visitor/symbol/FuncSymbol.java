package frontend.visitor.symbol;

import IR.type.IRType;
import frontend.lexer.Token;

public class FuncSymbol extends Symbol {
    public FuncSymbol(IRType type, Token ident) {
        super(type, ident.strVal(), ident.line());
    }
}
