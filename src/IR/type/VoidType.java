package IR.type;

public final class VoidType implements IRType {
    // 无类型，在SysY中仅作为自定义的函数的返回类型
    // 无类型的实例不能任意创建，需使用已经创建好的静态实例

    static final VoidType INSTANCE = new VoidType();

    private VoidType() {
    }

    @Override
    public String displayStr() {
        return "Void";
    }

    @Override
    public String llvmStr() {
        return "void";
    }

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        return this.llvmStr();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VoidType;
    }
}
