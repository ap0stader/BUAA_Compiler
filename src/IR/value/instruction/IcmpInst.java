package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.type.IntegerType;
import IR.value.IRBasicBlock;
import util.LLVMStrRegCounter;

// <result> = icmp `predicate` <ty> <op1>, <op2>
public class IcmpInst extends IRInstruction<IntegerType> {
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

    public IcmpInst(Predicate predicate, IRValue<IntegerType> operand1, IRValue<IntegerType> operand2, IRBasicBlock parent) {
        super(IRType.getInt1Ty(), parent);
        if (operand1.type().size() != operand2.type().size()) {
            throw new RuntimeException("When IcmpInst(), two operands size are mismatch. " +
                    "Got " + operand1.type().size() + " operand1 " + operand1 +
                    " and " + operand2.type().size() + " operand2 " + operand2);
        }
        this.predicate = predicate;
        this.addOperand(operand1);
        this.addOperand(operand2);
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        return counter.get(this) + " = icmp " + this.predicate + " " +
                this.getOperand(0).type().llvmStr() + " " +
                counter.get(this.getOperand(0)) + ", " +
                counter.get(this.getOperand(1));
    }
}
