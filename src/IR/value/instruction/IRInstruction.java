package IR.value.instruction;

import IR.IRUser;
import IR.type.IRType;
import IR.value.IRBasicBlock;
import util.LLVMStrRegCounter;

public abstract class IRInstruction<IT extends IRType> extends IRUser<IT> {
    private final IRBasicBlock parent;

    public IRInstruction(IT type, IRBasicBlock parent) {
        super(type);
        this.parent = parent;
        // 加入到BasicBlock中
        parent.appendInstruction(this);
    }

    public IRBasicBlock parent() {
        return parent;
    }

    public abstract String llvmStr(LLVMStrRegCounter counter);

    @Override
    public String toString() {
        return this.llvmStr(new LLVMStrRegCounter());
    }
}
