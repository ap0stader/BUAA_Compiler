package IR.value.instruction;

import IR.IRValue;
import IR.type.IntegerType;
import IR.type.PointerType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

// <result> = load <ty>, <ty>* <pointer>
public class LoadInst extends Instruction<IntegerType> {
    // 在SysY中，该指令仅在访问数组（包括参数中退化为指针的数组）发挥作用
    // 加载的类型与pointerOperand指向的类型相同，并且一定为IntegerType
    // LoadInst是UnaryInstruction的子类

    public LoadInst(IRValue<PointerType> pointerOperand, BasicBlock parent) {
        super(calcResultType(pointerOperand), parent);
        this.addOperand(pointerOperand);
    }

    private static IntegerType calcResultType(IRValue<PointerType> pointerOperand) {
        if (pointerOperand.type().referenceType() instanceof IntegerType referencedIntegerType) {
            return referencedIntegerType;
        } else {
            throw new RuntimeException("When calcResultType(), the referenceType of pointerOperand is not IntegerType. " +
                    "Got " + pointerOperand.type().referenceType() + " value " + pointerOperand);
        }
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
