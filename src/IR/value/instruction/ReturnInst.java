package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.type.IntegerType;
import IR.type.VoidType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

// ret <type> <value>
// ret void
public class ReturnInst extends Instruction<VoidType> {
    // 在SysY中，return的数值一定是IntegerType或者无返回值

    public ReturnInst(IRValue<IntegerType> returnValue, BasicBlock parent) {
        super(IRType.getVoidTy(), parent);
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
