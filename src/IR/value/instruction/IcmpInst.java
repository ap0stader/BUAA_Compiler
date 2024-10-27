package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.type.IntegerType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

// <result> = icmp <cond> <ty> <op1>, <op2>
public class IcmpInst extends Instruction {
    public enum Predicate {
        EQ("eq"),    // eq: equal
        NE("ne"),    // ne: not equal
        GT("sgt"),  // sgt: signed greater than
        GE("sge"),  // sge: signed greater or equal
        LT("slt"),  // slt: signed less than
        LE("sle");  // sle: signed less or equal

        private final String llvmStr;

        Predicate(String llvmStr) {
            this.llvmStr = llvmStr;
        }

        @Override
        public String toString() {
            return this.llvmStr;
        }
    }

    private final Predicate predicate;

    public IcmpInst(Predicate predicate, IRValue operand1, IRValue operand2, BasicBlock parent) {
        super(operand1.type(), parent);
        if (!IRType.isEqual(operand1.type(), operand2.type())) {
            throw new RuntimeException("When IcmpInst(), two operands types are mismatch. " +
                    "Got operand1: " + operand1 + " operand2: " + operand2);
        }
        if (!(operand1.type() instanceof IntegerType)) {
            throw new RuntimeException("When IcmpInst(), the type of operand1 is not IntegerType. Got " + operand1);
        }
        if (!(operand2.type() instanceof IntegerType)) {
            throw new RuntimeException("When IcmpInst(), the type of operand2 is not IntegerType. Got " + operand2);
        }
        this.predicate = predicate;
        this.addOperand(operand1);
        this.addOperand(operand2);
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        return counter.get(this) + " = icmp " + this.predicate + " " +
                this.type.llvmStr() + " " +
                counter.get(this.getOperand(0)) + ", " +
                counter.get(this.getOperand(1));
    }
}
