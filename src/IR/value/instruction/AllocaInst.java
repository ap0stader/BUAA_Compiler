package IR.value.instruction;

import IR.type.IRType;
import IR.type.PointerType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

// <result> = alloca <type>
public class AllocaInst extends Instruction<PointerType> {
    private final IRType allocType;

    public AllocaInst(IRType type, BasicBlock parent) {
        // 自动转为对应的指针类型，不需要在传入时包装为指针类型
        super(new PointerType(type, false), parent);
        this.allocType = type;
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        return counter.get(this) + " = alloca " + this.allocType.llvmStr();
    }
}
