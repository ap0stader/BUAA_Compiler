package IR.value.instruction;

import IR.type.IRType;
import IR.type.PointerType;
import IR.value.IRBasicBlock;
import util.LLVMStrRegCounter;

// <result> = alloca <type>
public class AllocaInst extends IRInstruction<PointerType> {
    private final IRType allocatedType;

    public AllocaInst(IRType type, IRBasicBlock parent) {
        // 自动转为对应的指针类型，不需要在传入时包装为指针类型
        super(new PointerType(type), parent);
        this.allocatedType = type;
    }

    public IRType allocatedType() {
        return allocatedType;
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        return counter.get(this) + " = alloca " + this.allocatedType.llvmStr();
    }
}
