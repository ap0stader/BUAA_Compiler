package IR;

import IR.type.IRType;

import java.util.HashSet;

public abstract class IRValue<T extends IRType> {
    protected final String name;
    protected final T type;
    // 使用HashSet防止有重复的use
    // WARNING 需十分留意User中有相同的操作数的情况下对于Use的删除操作
    private final HashSet<IRUse> useList;

    // 匿名初始化
    public IRValue(T type) {
        this.name = null;
        this.type = type;
        this.useList = new HashSet<>();
    }

    public IRValue(String name, T type) {
        this.name = name;
        this.type = type;
        this.useList = new HashSet<>();
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

    public HashSet<IRUse> useList() {
        return useList;
    }

    // 传入User，维护User-Use关系
    public void addUse(IRUser<?> user) {
        this.useList.add(new IRUse(user, this));
    }
}
