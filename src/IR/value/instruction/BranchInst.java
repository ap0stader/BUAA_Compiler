package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.type.IntegerType;
import IR.type.VoidType;
import IR.value.IRBasicBlock;
import util.LLVMStrRegCounter;

import java.util.Objects;

// br i1 <cond>, label <iftrue>, label <iffalse>
// br label <dest>
public class BranchInst extends IRInstruction<VoidType> {
    // 有条件跳转
    public BranchInst(IRValue<IntegerType> cond, IRBasicBlock ifTrue, IRBasicBlock ifFalse, IRBasicBlock parent) {
        super(IRType.getVoidTy(), parent);
        if (!IRType.isEqual(cond.type(), IRType.getInt1Ty())) {
            throw new RuntimeException("When BranchInst(), the type of cond is not i1. Got " + cond.type() + " value " + cond);
        }
        this.addOperand(cond);
        this.addOperand(ifTrue);
        this.addOperand(ifFalse);
    }

    // 无条件跳转
    public BranchInst(IRBasicBlock dest, IRBasicBlock parent) {
        super(IRType.getVoidTy(), parent);
        this.addOperand(dest);
    }

    public boolean isConditional() {
        return this.getNumOperands() == 3;
    }

    public IRValue<IntegerType> getCondition() {
        // CAST 构造函数限制
        if (this.isConditional()) {
            return IRValue.cast(this.getOperand(0));
        } else {
            throw new RuntimeException("When getCondition(), the branchInst is unconditional.");
        }
    }

    public IRBasicBlock getTrueSuccessor() {
        // CAST 构造函数限制
        if (this.isConditional()) {
            return (IRBasicBlock) this.getOperand(1);
        } else {
            throw new RuntimeException("When getTrueSuccessor(), the branchInst is unconditional.");
        }
    }

    public IRBasicBlock getFalseSuccessor() {
        // CAST 构造函数限制
        if (this.isConditional()) {
            return (IRBasicBlock) this.getOperand(2);
        } else {
            throw new RuntimeException("When getFalseSuccessor(), the branchInst is unconditional.");
        }
    }

    public IRBasicBlock getSuccessor() {
        // CAST 构造函数限制
        if (!this.isConditional()) {
            return (IRBasicBlock) this.getOperand(0);
        } else {
            throw new RuntimeException("When getSuccessor(), the branchInst is conditional.");
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
            throw new RuntimeException("When BranchInst.llvmStr(), the number of operands(" + this.getNumOperands() + ") is incorrect, expected 3 or 1.");
        }
    }
}
