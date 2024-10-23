package IR.value;

import IR.IRValue;
import IR.type.IRType;
import IR.value.constant.Constant;

public class GlobalVariable extends IRValue {
    private final boolean isConstant;
    private final boolean isPrivate;
    private final Constant initVals;

    public GlobalVariable(IRType type, String name,
                          boolean setConstant, boolean setPrivate,
                          Constant initVals) {
        super(name, type);
        this.isConstant = setConstant;
        this.isPrivate = setPrivate;
        this.initVals = initVals;
    }

    @Override
    public String toString() {
        return "@" + this.name + " = " +
                (this.isPrivate ? "private unnamed_addr" : "dso_local") + " " +
                (this.isConstant ? "constant" : "global") + " " +
                this.initVals.toString();
    }
}
