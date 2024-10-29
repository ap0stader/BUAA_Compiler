package IR.type;

public final class LabelType implements IRType {
    // 标签类型，专门用于LLVM IR的BasicBlock
    // 标签类型的实例不能任意创建，需使用已经创建好的静态实例

    static final LabelType INSTANCE = new LabelType();

    private LabelType() {
    }

    // 这不是LLVM中实现的一个类，仅用于BasicBlock
    @Override
    public String displayStr() {
        throw new UnsupportedOperationException("A LabelType should not be display in SysY.");
    }

    @Override
    public String llvmStr() {
        return "label";
    }

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        return this.llvmStr();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LabelType;
    }
}
