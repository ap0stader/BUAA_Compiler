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
        // 针对位宽进行处理
        int integerBitWidth = type.getBitWidth();
        if (integerBitWidth < IRType.getInt32Ty().getBitWidth()) {
            int lowerMask = (1 << integerBitWidth) - 1;
            int lowerBit = constantValue & lowerMask;
            if ((lowerBit & (1 << (integerBitWidth - 1))) != 0) {
                lowerBit = lowerBit | ~lowerMask;
            }
            this.constantValue = lowerBit;
        } else {
            this.constantValue = constantValue;
        }
    }

    public Integer constantValue() {
        return constantValue;
    }

    @Override
    public String llvmStr() {
        return this.constantValue.toString();
    }
}
