package IR.type;

public final class VoidType implements IRType {
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
