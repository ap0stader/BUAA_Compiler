package IR;

import IR.type.IRType;

public abstract class IRValue {
    protected final String name;
    protected final IRType type;

    // TODO：未完成开发时使用，后续应当删除
    public IRValue() {
        throw new UnsupportedOperationException("Unimplemented.");
    }

    public IRValue(IRType type) {
        this.name = null;
        this.type = type;
    }

    public IRValue(String name, IRType type) {
        this.name = name;
        this.type = type;
    }

    public String name() {
        return name;
    }

    public IRType type() {
        return type;
    }

    // TODO：未完成开发时使用，后续应当改为抽象方法
    public String llvmStr() {
        throw new UnsupportedOperationException("Unimplemented.");
    }
}
