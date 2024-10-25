package IR.value;

import IR.IRValue;
import IR.type.IRType;
import util.LLVMStrRegCounter;

public class Argument extends IRValue {
    public Argument(IRType.VarSymbolType type) {
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
