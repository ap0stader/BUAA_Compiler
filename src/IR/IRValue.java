package IR;

import IR.type.IRType;

import java.util.LinkedList;
import java.util.Objects;

public abstract class IRValue<T extends IRType> {
    protected final String name;
    protected final T type;
    private final LinkedList<IRUse> useList;

    // 匿名初始化
    public IRValue(T type) {
        this.name = null;
        this.type = type;
        this.useList = new LinkedList<>();
    }

    public IRValue(String name, T type) {
        this.name = name;
        this.type = type;
        this.useList = new LinkedList<>();
    }

    @SuppressWarnings("unchecked")
    public static <U extends IRType> IRValue<U> cast(IRValue<?> value) {
        // CAST 在确保合理的情况下，强制确定IRValue的类型
        return (IRValue<U>) value;
    }

    public String name() {
        return name;
    }

    public T type() {
        return type;
    }

    public LinkedList<IRUse> useList() {
        return useList;
    }

    // 传入User，维护User-Use关系
    public void addUse(IRUser<?> user) {
        this.useList.add(new IRUse(user, this));
    }

    // 传入User，删除所有该User的Use
    public void removeUserAllUse(IRUser<?> user) {
        this.useList.removeIf(use -> Objects.equals(use.user(), user));
    }
}
