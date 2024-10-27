package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

import java.util.Objects;

// br i1 <cond>, label <iftrue>, label <iffalse>
// br label <dest>
public class BranchInst extends Instruction {
    // 有条件跳转
    public BranchInst(IRValue cond, BasicBlock ifTrue, BasicBlock ifFalse, BasicBlock parent) {
        super(IRType.getVoidTy(), parent);
        if (!IRType.isEqual(cond.type(), IRType.getInt1Ty())) {
            throw new RuntimeException("When BranchInst(), the type of cond is not i1. Got " + cond);
        }
        if (!Objects.equals(ifTrue.parent(), parent.parent())) {
            throw new RuntimeException("When BranchInst(), the parent of ifTrue is not the function where the instruction in. " +
                    "Instruction: " + parent.parent().name() + " ifTrue: " + ifTrue.parent().name());
        }
        if (!Objects.equals(ifFalse.parent(), parent.parent())) {
            throw new RuntimeException("When BranchInst(), the parent of ifFalse is not the function where the instruction in. " +
                    "Instruction: " + parent.parent().name() + " ifFalse: " + ifFalse.parent().name());
        }
        this.addOperand(cond);
        this.addOperand(ifTrue);
        this.addOperand(ifFalse);
    }

    // 无条件跳转
    public BranchInst(BasicBlock dest, BasicBlock parent) {
        super(IRType.getVoidTy(), parent);
        if (!IRType.isEqual(dest.type(), IRType.getInt1Ty())) {
            throw new RuntimeException("When BranchInst(), the parent of dest is not the function where the instruction in. " +
                    "Instruction: " + parent.parent().name() + " dest: " + dest.parent().name());
        }
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        if (this.getNumOperands() == 3) {
            return "br " +
                    this.getOperand(0).type().llvmStr() + " " +
                    counter.get(this.getOperand(0)) + ", " +
                    this.getOperand(1).type().llvmStr() + " " +
                    counter.get(this.getOperand(1)) + ", " +
                    this.getOperand(2).type().llvmStr() + " " +
                    counter.get(this.getOperand(2));
        } else if (this.getNumOperands() == 1) {
            return "br " +
                    this.getOperand(0).type().llvmStr() + " " +
                    counter.get(this.getOperand(0));
        } else {
            throw new RuntimeException("When llvmStr(), the number of operands(" + this.getNumOperands() + ") is incorrect, expected 3 or 1.");
        }
    }
}
