package frontend.visitor.symbol;

import IR.type.IRType;
import frontend.lexer.Token;

public class FuncSymbol extends Symbol {
    protected FuncSymbol(IRType.FuncSymbolType type, Token ident) {
        super(type, ident);
    }
}
