package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.type.PointerType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

// <result> = load <ty>, <ty>* <pointer>
public class LoadInst extends Instruction {
    // load指令加载pointer指向的数据，但是加载的类型不要求与pointer指向的类型相同
    // 但是在Sysy中，该指令仅在访问数组（包括参数中退化为指针的数组）发挥作用，此时加载的类型与pointer指向的类型一定相同
    // LoadInst是UnaryInstruction的子类
    public LoadInst(IRValue pointerOperand, BasicBlock parent) {
        super(calcResultType(pointerOperand), parent);
        this.addOperand(pointerOperand);
    }

    private static IRType calcResultType(IRValue pointerOperand) {
        if (pointerOperand.type() instanceof PointerType pointerType) {
            return pointerType.referenceType();
        } else {
            throw new RuntimeException("When calcResultType(), the type of pointerOperand is not PointerType. Got " + pointerOperand);
        }
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        return counter.get(this) +
                " = load " +
                this.type.llvmStr() + ", " +
                this.getOperand(0).type().llvmStr() + " " +
                counter.get(this.getOperand(0)) + " ";
    }
}
