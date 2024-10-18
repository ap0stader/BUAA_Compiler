package frontend.visitor.symbol;

import IR.type.IRType;
import frontend.lexer.Token;

import java.util.ArrayList;

public class VarSymbol extends Symbol {
    private final boolean isConst;
    private final ArrayList<Integer> initVals;

    public VarSymbol(IRType type, Token ident, boolean isConst, ArrayList<Integer> initVals) {
        super(type, ident.strVal(), ident.line());
        this.isConst = isConst;
        this.initVals = initVals;
    }
}
