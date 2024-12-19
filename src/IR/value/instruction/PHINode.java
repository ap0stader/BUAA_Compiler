package IR.value.instruction;

import IR.IRValue;
import IR.type.IntegerType;
import IR.value.IRBasicBlock;
import util.LLVMStrRegCounter;

public class PHINode extends IRInstruction<IntegerType> {
    public PHINode(IntegerType allocaType, IRBasicBlock parent) {
        super(allocaType, null);
        // 不传入参数，这样就不会自动添加到尾部
        this.setParent(parent);
        this.parent.pushInstruction(this);
    }

    public void addIncoming(IRValue<?> value, IRBasicBlock basicBlock) {
        this.addOperand(value);
        this.addOperand(basicBlock);
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        if (this.getNumOperands() % 2 == 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(counter.get(this)).append(" = phi ").append(this.type.llvmStr()).append(" ");
            for (int i = 0; i < this.getNumOperands(); i = i + 2) {
                sb.append(i > 0 ? ", [ " : "[ ");
                sb.append(counter.get(this.getOperand(i))).append(", ");
                sb.append(counter.get(this.getOperand(i + 1)));
                sb.append(" ]");
            }
            return sb.toString();
        } else {
            throw new RuntimeException("When PHINode.llvmStr(), the number of operands(" + this.getNumOperands() + ") is incorrect, expected an even number.");
        }
    }
}
