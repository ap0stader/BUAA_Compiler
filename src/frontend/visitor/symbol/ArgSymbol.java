package frontend.visitor.symbol;

import IR.IRValue;
import IR.type.PointerType;
import IR.value.Argument;
import frontend.lexer.Token;
import frontend.type.Symbol;

public class ArgSymbol extends Symbol<SymbolType.Arg, IRValue<PointerType>> {
    private final Argument argument;

    public ArgSymbol(SymbolType.Arg type, Token ident) {
        super(type, ident);
        this.argument = new Argument(type);
    }

    public Argument argument() {
        return argument;
    }
}
