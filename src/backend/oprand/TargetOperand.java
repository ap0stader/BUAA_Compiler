package backend.oprand;

public abstract class TargetOperand {
    // LLVM IR的核心是Value，MIPS汇编的核心是操作数
    public abstract String mipsStr();

    @Override
    public String toString() {
        return this.mipsStr();
    }
}
