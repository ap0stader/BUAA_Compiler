package frontend.visitor.symbol;

import frontend.lexer.Token;

public class ArgSymbol extends Symbol<SymbolType.Arg, SymbolType.Arg> {
    public ArgSymbol(SymbolType.Arg type, Token ident) {
        super(type, ident);
    }
}
