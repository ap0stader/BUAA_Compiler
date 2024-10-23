package IR.value.constant;

import IR.IRValue;
import IR.type.IRType;

public abstract class Constant extends IRValue {
    protected Constant(IRType type) {
        super(type);
    }

    public abstract String llvmStr();
}
