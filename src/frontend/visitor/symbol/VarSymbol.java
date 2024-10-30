package frontend.visitor.symbol;

import IR.IRValue;
import IR.type.PointerType;
import frontend.lexer.Token;

public class VarSymbol<VT extends IRValue<PointerType>> extends Symbol<SymbolType.Var, VT> {
    // VarSymbol中存储的只可能是GlobalVariable和AllocaInst，所以irValue的类型为PointerType
    public VarSymbol(SymbolType.Var type, Token ident) {
        super(type, ident);
    }
}
