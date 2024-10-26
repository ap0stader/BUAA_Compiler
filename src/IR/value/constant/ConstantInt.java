package IR.value.constant;

import IR.type.IntegerType;

public class ConstantInt extends Constant {
    private final Integer constantValue;

    public ConstantInt(IntegerType type, Integer constantValue) {
        super(type);
        this.constantValue = constantValue;
    }

    @Override
    public String llvmStr() {
        return this.constantValue.toString();
    }
}
