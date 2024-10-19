package frontend.visitor.symbol;

import IR.type.IRType;
import frontend.lexer.Token;

public class VarSymbol extends Symbol {
    public VarSymbol(IRType.VarSymbolType type, Token ident) {
        super(type, ident);
    }
}
