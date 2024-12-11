package IR.value.instruction;

import IR.IRValue;
import IR.type.IntegerType;
import IR.value.IRBasicBlock;
import util.LLVMStrRegCounter;

// <result> = `operator` <ty> <op1>, <op2>
public class BinaryOperator extends IRInstruction<IntegerType> {
    public enum BinaryOps {
        ADD("add"),
        SUB("sub"),
        MUL("mul"),
        DIV("sdiv"),
        MOD("srem");

        private final String llvmStr;

        BinaryOps(String llvmStr) {
            this.llvmStr = llvmStr;
        }

        @Override
        public String toString() {
            return this.llvmStr;
        }
    }

    private final BinaryOps binaryOp;

    public BinaryOps binaryOp() {
        return binaryOp;
    }

    // BinaryOperator要求两个operand的类型相等并且是可运算类型（在Sysy中为IntegerType），返回的类型也为operand的类型
    public BinaryOperator(BinaryOps binaryOp, IRValue<IntegerType> operand1, IRValue<IntegerType> operand2, IRBasicBlock parent) {
        super(operand1.type(), parent);
        if (operand1.type().size() != operand2.type().size()) {
            throw new RuntimeException("When BinaryOperator(), two operands sizes are mismatch. " +
                    "Got " + operand1.type().size() + " operand1 " + operand1 +
                    " and " + operand2.type().size() + " operand2 " + operand2);
        }
        this.binaryOp = binaryOp;
        this.addOperand(operand1);
        this.addOperand(operand2);
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        return counter.get(this) + " = " + this.binaryOp + " " +
                this.type.llvmStr() + " " +
                counter.get(this.getOperand(0)) + ", " +
                counter.get(this.getOperand(1));
    }
}
