package IR.value.constant;

import IR.type.IRType;
import IR.type.IntegerType;

public class ConstantInt extends IRConstant<IntegerType> {
    private final Integer constantValue;

    // 在多处需要使用到i32类型的0
    public static ConstantInt ZERO_I32() {
        return new ConstantInt(IRType.getInt32Ty(), 0);
    }

    public ConstantInt(IntegerType type, Integer constantValue) {
        super(type);
        this.constantValue = constantValue;
    }

    public Integer constantValue() {
        return constantValue;
    }

    @Override
    public String llvmStr() {
        int integerSize = this.type.size();
        // 根据type的size进行截断
        if (integerSize < 32) {
            int lowerMask = (1 << integerSize) - 1;
            int lowerBit = this.constantValue & lowerMask;
            if ((lowerBit & (1 << (integerSize - 1))) != 0) {
                lowerBit = lowerBit | ~lowerMask;
            }
            return String.valueOf(lowerBit);
        } else {
            return this.constantValue.toString();
        }
    }
}
