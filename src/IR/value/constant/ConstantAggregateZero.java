package IR.value.constant;

import IR.type.IRType;

public class ConstantAggregateZero extends Constant {
    public ConstantAggregateZero(IRType type) {
        super(type);
    }

    @Override
    public String llvmStr() {
        return this.type.llvmStr() + " zeroinitializer";
    }
}
