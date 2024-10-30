package frontend.visitor.symbol;

import IR.value.Argument;
import frontend.lexer.Token;

public class ArgSymbol extends Symbol<SymbolType.Arg, Argument> {
    public ArgSymbol(SymbolType.Arg type, Token ident) {
        super(type, ident);
    }
}
