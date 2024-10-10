package frontend.visitor.symbol;

import IR.type.Type;
import frontend.lexer.Token;

import java.util.ArrayList;

public class VarSymbol extends Symbol {
    private final boolean isConst;
    private final ArrayList<Integer> initVals;

    public VarSymbol(Type type, Token ident, boolean isConst, ArrayList<Integer> initVals) {
        super(type, ident.strVal(), ident.line());
        this.isConst = isConst;
        this.initVals = initVals;
    }
}
