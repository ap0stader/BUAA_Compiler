package frontend.visitor.symbol;

import IR.value.Argument;
import IR.value.instruction.AllocaInst;
import frontend.lexer.Token;
import frontend.type.Symbol;

public class ArgSymbol extends Symbol<SymbolType.Arg, AllocaInst> {
    private final Argument argument;

    public ArgSymbol(SymbolType.Arg type, Token ident) {
        super(type, ident);
        this.argument = new Argument(type);
    }

    public Argument argument() {
        return argument;
    }
}
