package IR.value.constant;

import IR.type.IRType;
import IR.type.IntegerType;

public class ConstantInt extends Constant {
    private final Integer constantValue;

    public ConstantInt(IntegerType type, Integer constantValue) {
        super(type);
        this.constantValue = constantValue;
    }

    public static ConstantInt zero_i32() {
        return new ConstantInt(IRType.getInt32Ty(), 0);
    }

    @Override
    public String llvmStr() {
        int integerSize = ((IntegerType) this.type).size();
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
