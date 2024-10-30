package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.type.IntegerType;
import IR.type.PointerType;
import IR.type.VoidType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

// store <ty> <value>, <ty>* <pointer>
public class StoreInst extends Instruction<VoidType> {
    // 在SysY中，store的数值一定是IntegerType
    //

    public StoreInst(IRValue<IntegerType> valueOperand, IRValue<PointerType> pointerOperand, BasicBlock parent) {
        super(IRType.getVoidTy(), parent);
        if (!IRType.isEqual(valueOperand.type(), pointerOperand.type().referenceType())) {
            throw new RuntimeException("When StoreInst(), the pointerOperand is not a pointer to valueOperand. " +
                    "Got " + valueOperand.type() + " valueOperand " + valueOperand +
                    " and " + pointerOperand.type() + " pointerOperand " + pointerOperand);
        }
        this.addOperand(valueOperand);
        this.addOperand(pointerOperand);
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        return "store " +
                this.getOperand(0).type().llvmStr() + " " + counter.get(this.getOperand(0)) + ", " +
                this.getOperand(1).type().llvmStr() + " " + counter.get(this.getOperand(1));
    }
}
