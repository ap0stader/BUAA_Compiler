package IR.value.instruction;

import IR.IRUser;
import IR.type.IRType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

public abstract class Instruction extends IRUser {
    private final BasicBlock parent;

    public Instruction(IRType type, BasicBlock parent) {
        super(type);
        this.parent = parent;
        // 加入到BasicBlock中
        parent.appendInstruction(this);
    }

    public abstract String llvmStr(LLVMStrRegCounter counter);

    @Override
    public String toString() {
        return this.llvmStr(new LLVMStrRegCounter());
    }
}
