package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.value.IRBasicBlock;
import util.LLVMStrRegCounter;
import util.Pair;

import java.util.ArrayList;

public class PHINode extends IRInstruction<IRType> {
    public PHINode(IRType allocaType, IRBasicBlock parent) {
        // 不给super传入parent参数，这样就不会自动添加到尾部
        super(allocaType, null);
        this.setParent(parent);
        this.parent.pushInstruction(this);
    }

    public void addIncoming(IRValue<?> value, IRBasicBlock basicBlock) {
        this.addOperand(value);
        this.addOperand(basicBlock);
    }

    public ArrayList<Pair<IRBasicBlock, IRValue<?>>> getIncomingBlockValuePairs() {
        if (this.getNumOperands() % 2 == 0) {
            ArrayList<Pair<IRBasicBlock, IRValue<?>>> pairs = new ArrayList<>();
            for (int i = 0; i < this.getNumOperands(); i = i + 2) {
                // CAST 构造函数限制
                pairs.add(new Pair<>((IRBasicBlock) this.getOperand(i + 1), this.getOperand(i)));
            }
            return pairs;
        } else {
            throw new RuntimeException("getIncomingBlockValuePairs, the number of operands(" + this.getNumOperands() + ") is incorrect, expected an even number.");
        }
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
