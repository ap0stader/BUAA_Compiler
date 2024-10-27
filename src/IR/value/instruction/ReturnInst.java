package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

// ret <type> <value>
// ret void
public class ReturnInst extends Instruction {
    public ReturnInst(IRValue returnValue, BasicBlock block) {
        super(IRType.getVoidTy(), block);
        if (returnValue != null) {
            this.addOperand(returnValue);
        }
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        if (this.getNumOperands() == 0) {
            return "ret void";
        } else {
            return "ret " +
                    this.getOperand(0).type().llvmStr() + " " +
                    counter.get(this.getOperand(0));
        }
    }
}
