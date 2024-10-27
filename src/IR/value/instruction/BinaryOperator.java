package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

// <result> = `operator` <ty> <op1>, <op2>
public class BinaryOperator extends Instruction {
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

    // BinaryOperator要求两个operand的类型相等，返回的类型也为operand的类型
    public BinaryOperator(BinaryOps binaryOp, IRValue operand1, IRValue operand2, BasicBlock parent) {
        super(operand1.type(), parent);
        if (!IRType.isEqual(operand1.type(), operand2.type())) {
            throw new RuntimeException("When BinaryOperator(), two operands types are mismatch. " +
                    "Got operand1: " + operand1 + " operand2: " + operand2);
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
