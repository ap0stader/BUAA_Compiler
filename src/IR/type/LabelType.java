package IR.type;

public record LabelType() implements IRType {
    // 这不是LLVM中实现的一个类，仅用于BasicBlock
    @Override
    public String displayStr() {
        throw new UnsupportedOperationException("A LabelType should not be display in SysY.");
    }

    @Override
    public String llvmStr() {
        throw new UnsupportedOperationException("A LabelType have no LLVM represent.");
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LabelType;
    }
}
