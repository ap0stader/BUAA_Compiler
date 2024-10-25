package frontend.visitor.symbol;

import IR.type.FunctionType;
import frontend.lexer.Token;

public class FuncSymbol extends Symbol<FunctionType> {
    public FuncSymbol(FunctionType type, Token ident) {
        super(type, ident);
    }
}
