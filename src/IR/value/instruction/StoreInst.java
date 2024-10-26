package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.type.PointerType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

// store <ty> <value>, <ty>* <pointer>
public class StoreInst extends Instruction {
    public StoreInst(IRValue valueOperand, IRValue pointerOperand, BasicBlock parent) {
        super(IRType.getVoidTy(), parent);
        this.addOperand(valueOperand);
        if (pointerOperand.type() instanceof PointerType) {
            this.addOperand(pointerOperand);
        } else {
            throw new RuntimeException("When StoreInst(), the type of pointerOperand is not PointerType. Got " + pointerOperand);
        }
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        return "store " +
                this.getOperand(0).type().llvmStr() + " " + counter.get(this.getOperand(0)) + ", " +
                this.getOperand(1).type().llvmStr() + " " + counter.get(this.getOperand(1));
    }
}
