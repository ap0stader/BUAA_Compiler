package backend.oprand;

public interface TargetOperand {
    // LLVM IR的核心是Value，MIPS汇编的核心是操作数
    String mipsStr();
}
