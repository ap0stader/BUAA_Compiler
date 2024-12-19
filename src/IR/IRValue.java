package IR;

import IR.type.IRType;

import java.util.HashSet;

public abstract class IRValue<T extends IRType> {
    protected final String name;
    protected final T type;
    protected final HashSet<IRUser<?>> users;

    // 匿名初始化
    public IRValue(T type) {
        this.name = null;
        this.type = type;
        this.users = new HashSet<>();
    }

    public IRValue(String name, T type) {
        this.name = name;
        this.type = type;
        this.users = new HashSet<>();
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

    public HashSet<IRUser<?>> users() {
        return users;
    }

    public void addUser(IRUser<?> user) {
        this.users.add(user);
    }

    public void removeUser(IRUser<?> user) {
        this.users.remove(user);
    }

    public void replaceAllUsesWith(IRValue<?> value) {
        HashSet<IRUser<?>> tempUser = new HashSet<>(this.users);
        tempUser.forEach(user -> user.replaceUsesOfWith(this, value));
    }
}
