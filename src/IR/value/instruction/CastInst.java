package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.type.IntegerType;
import IR.value.BasicBlock;
import util.LLVMStrRegCounter;

public abstract class CastInst<D extends IRType> extends Instruction<D> {
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

    // CastInst是UnaryInstruction的子类
    private final CastOps castOp;

    private CastInst(CastOps castOp, IRValue<?> src, D destType, BasicBlock parent) {
        super(destType, parent);
        this.castOp = castOp;
        this.addOperand(src);
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        return counter.get(this) + " = " + this.castOp + " " +
                this.getOperand(0).type().llvmStr() + " " + counter.get(this.getOperand(0)) +
                " to " + this.type.llvmStr();
    }

    // <result> = trunc <ty> <value> to <ty2>
    public static class TruncInst extends CastInst<IntegerType> {
        public TruncInst(IRValue<IntegerType> src, IntegerType destType, BasicBlock parent) {
            super(CastOps.TRUNC, src, destType, parent);
        }
    }

    // <result> = zext <ty> <value> to <ty2>
    public static class ZExtInst extends CastInst<IntegerType> {
        public ZExtInst(IRValue<IntegerType> src, IntegerType destType, BasicBlock parent) {
            super(CastOps.ZEXT, src, destType, parent);
        }
    }

    // <result> = bitcast <ty> <value> to <ty2>
    public static class BitCastInst<D extends IRType> extends CastInst<D> {
        public BitCastInst(IRValue<?> src, D destType, BasicBlock parent) {
            super(CastOps.BITCAST, src, destType, parent);
        }
    }
}
