package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

public abstract class CastInst extends Instruction {
    protected enum CastOps {
        TRUNC("trunc"),
        ZEXT("zext"),
        BITCAST("bitcast");

        private final String llvmStr;

        CastOps(String llvmStr) {
            this.llvmStr = llvmStr;
        }

        @Override
        public String toString() {
            return this.llvmStr;
        }
    }

    // CastInst是UnaryInstruction的子类，只使用operands.get(0)
    private final CastOps castOp;

    private CastInst(CastOps castOp, IRType destType, IRType srcType, IRValue operand, BasicBlock parent) {
        super(destType, parent);
        this.castOp = castOp;
        this.addOperand(operand);
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        return counter.get(this) + " = " + this.castOp + " " +
                this.getOperand(0).type().llvmStr() + " " + counter.get(this.getOperand(0)) +
                " to " + this.type.llvmStr();
    }

    // <result> = trunc <ty> <value> to <ty2>
    public class TruncInst extends CastInst {
        public TruncInst(IRType destType, IRType srcType, IRValue operand, BasicBlock parent) {
            super(CastOps.TRUNC, destType, srcType, operand, parent);
        }
    }

    // <result> = zext <ty> <value> to <ty2>
    public class ZExtInst extends CastInst {
        public ZExtInst(IRType destType, IRType srcType, IRValue operand, BasicBlock parent) {
            super(CastOps.ZEXT, destType, srcType, operand, parent);
        }
    }

    // <result> = bitcast <ty> <value> to <ty2>
    public class BitCastInst extends CastInst {
        public BitCastInst(IRType destType, IRType srcType, IRValue operand, BasicBlock parent) {
            super(CastOps.BITCAST, destType, srcType, operand, parent);
        }
    }
}
