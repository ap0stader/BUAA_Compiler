package IR.value;

import IR.IRValue;
import IR.type.IRType;
import IR.type.PointerType;
import IR.value.constant.Constant;

public class GlobalVariable extends IRValue {
    private final boolean isConstant;
    private final boolean isPrivate;
    private final Constant initVals;

    public GlobalVariable(String name, IRType type,
                          boolean setConstant, boolean setPrivate,
                          Constant initVals) {
        // 自动转为对应的指针类型，不需要在传入时包装为指针类型
        super(name, new PointerType(type, false));
        this.isConstant = setConstant;
        this.isPrivate = setPrivate;
        this.initVals = initVals;
    }

    public String llvmStr() {
        return "@" + this.name + " = " +
                (this.isPrivate ? "private unnamed_addr" : "dso_local") + " " +
                (this.isConstant ? "constant" : "global") + " " +
                this.initVals.llvmStr() + "\n";
    }

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        return this.llvmStr();
    }
}
