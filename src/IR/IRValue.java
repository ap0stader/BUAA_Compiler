package IR;

import IR.type.IRType;

import java.util.HashSet;

public abstract class IRValue {
    protected final String name;
    protected final IRType type;
    // 使用HashSet防止有重复的use
    // WARN 需十分留意User中有相同的
    private final HashSet<IRUse> useList;

    public IRValue(IRType type) {
        this.name = null;
        this.type = type;
        this.useList = new HashSet<>();
    }

    public IRValue(String name, IRType type) {
        this.name = name;
        this.type = type;
        this.useList = new HashSet<>();
    }

    public String name() {
        return name;
    }

    public IRType type() {
        return type;
    }

    public HashSet<IRUse> useList() {
        return useList;
    }

    // 传入User，维护User-Use关系
    // TODO 界定可见性范围
    protected void addUse(IRUser user) {
        this.useList.add(new IRUse(user, this));
    }
}
