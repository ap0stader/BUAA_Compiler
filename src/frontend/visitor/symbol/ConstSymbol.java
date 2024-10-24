package frontend.visitor.symbol;

import IR.type.IRType;
import frontend.lexer.Token;
import global.Config;

import java.util.ArrayList;

public class ConstSymbol extends Symbol {
    private final ArrayList<Integer> initVals;

    public ConstSymbol(IRType.ConstSymbolType type, Token ident, ArrayList<Integer> initVals) {
        super(type, ident);
        this.initVals = initVals;
    }

    public ArrayList<Integer> initVals() {
        return initVals;
    }

    public Integer getInitValAtIndex(Token ident, int index) {
        if (index >= initVals.size()) {
            if (Config.visitorThrowable) {
                throw new IndexOutOfBoundsException("When getInitValAtIndex(), accessed by " + ident
                        + ", index " + index + " out of bound " + initVals.size());
            } else {
                return 0;
            }
        }
        return this.initVals.get(index);
    }
}
