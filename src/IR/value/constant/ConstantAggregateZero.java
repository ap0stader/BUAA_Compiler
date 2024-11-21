package IR.value.constant;

import IR.type.IRType;

public class ConstantAggregateZero extends IRConstant<IRType> {
    public ConstantAggregateZero(IRType type) {
        super(type);
    }

    @Override
    public String llvmStr() {
        return "zeroinitializer";
    }
}
