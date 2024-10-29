package IR.value;

import IR.IRValue;
import frontend.visitor.symbol.SymbolType;
import util.LLVMStrRegCounter;

// 由于参数是直接使用的，符号表的登记类型与Argument的类型是一致的，所以此处限制为SymbolType.Arg
public class Argument extends IRValue<SymbolType.Arg> {
    public Argument(SymbolType.Arg type) {
        super(type);
    }

    public String llvmStr(LLVMStrRegCounter counter) {
        return this.type.llvmStr() + " " + counter.get(this);
    }

    @Override
    public String toString() {
        return this.llvmStr(new LLVMStrRegCounter());
    }
}
