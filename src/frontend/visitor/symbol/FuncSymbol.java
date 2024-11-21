package frontend.visitor.symbol;

import IR.type.FunctionType;
import IR.value.IRFunction;
import frontend.lexer.Token;
import frontend.type.Symbol;

public class FuncSymbol extends Symbol<FunctionType, IRFunction> {
    public FuncSymbol(FunctionType type, Token ident) {
        super(type, ident);
    }
}
