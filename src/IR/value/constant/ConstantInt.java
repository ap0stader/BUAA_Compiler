package IR.value.constant;

import IR.type.IntegerType;

public class ConstantInt extends Constant {
    private final int constantValue;

    public ConstantInt(IntegerType type, int constantValue) {
        super(type);
        this.constantValue = constantValue;
    }

    @Override
    public String llvmStr() {
        return this.type.llvmStr() + " " + this.constantValue;
    }
}
