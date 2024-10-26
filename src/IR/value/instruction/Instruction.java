package IR.value.instruction;

import IR.IRUser;
import IR.type.IRType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

public abstract class Instruction extends IRUser {
    private final BasicBlock parent;

    // TODO：未完成开发时使用，后续应当删除
    public Instruction() {
        super(null);
        throw new UnsupportedOperationException("Unimplemented");
    }

    public Instruction(IRType type, BasicBlock parent) {
        super(type);
        this.parent = parent;
        // 加入到BasicBlock中
        parent.appendInstruction(this);
    }

    // TODO：未完成开发时使用，后续应当改为抽象方法
    public String llvmStr(LLVMStrRegCounter counter) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public String toString() {
        return this.llvmStr(new LLVMStrRegCounter());
    }
}
