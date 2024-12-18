package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.type.PointerType;
import IR.value.IRBasicBlock;
import util.LLVMStrRegCounter;

// <result> = load <ty>, <ty>* <pointer>
public class LoadInst extends IRInstruction<IRType> {
    // 在SysY中，该指令仅在访问数组（包括参数中退化为指针的数组）发挥作用
    // 加载的类型与pointerOperand指向的类型相同
    // LoadInst是UnaryInstruction的子类

    public LoadInst(IRValue<PointerType> pointerOperand, IRBasicBlock parent) {
        super(pointerOperand.type().referenceType(), parent);
        this.addOperand(pointerOperand);
    }

    // CAST 构造函数限制
    public IRValue<PointerType> getPointerOperand() {
        return IRValue.cast(this.getOperand(0));
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        return counter.get(this) +
                " = load " +
                this.type.llvmStr() + ", " +
                this.getOperand(0).type().llvmStr() + " " +
                counter.get(this.getOperand(0));
    }
}
