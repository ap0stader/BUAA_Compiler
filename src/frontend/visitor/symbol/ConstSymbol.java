package frontend.visitor.symbol;

import IR.IRValue;
import IR.type.PointerType;
import frontend.lexer.Token;
import global.Config;

import java.util.ArrayList;

public class ConstSymbol<VT extends IRValue<PointerType>> extends Symbol<SymbolType.Const, VT> {
    // ConstSymbol中存储的只可能是GlobalVariable和AllocaInst，所以irValue的类型为PointerType
    // 由于只考虑一维数组，所以此处直接线性保存
    private final ArrayList<Integer> initVals;

    public ConstSymbol(SymbolType.Const type, Token ident, ArrayList<Integer> initVals) {
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
