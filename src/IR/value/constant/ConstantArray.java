package IR.value.constant;

import IR.type.ArrayType;
import IR.type.IRType;

import java.util.ArrayList;

public class ConstantArray extends IRConstant<ArrayType> {
    private final ArrayList<IRConstant<?>> constantValues;

    public ConstantArray(ArrayType arrayType, ArrayList<IRConstant<?>> constantValues) {
        super(arrayType);
        // 检查类型是否匹配
        for (IRConstant<?> value : constantValues) {
            if (!IRType.isEqual(arrayType.elementType(), value.type())) {
                throw new RuntimeException("When ConstantArray(), type mismatch. Got " + value.type() + " value " + value +
                        ", expected " + arrayType.elementType());
            }
        }
        this.constantValues = constantValues;
    }

    public ArrayList<IRConstant<?>> constantValues() {
        return constantValues;
    }

    @Override
    public String llvmStr() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < this.constantValues.size(); i++) {
            sb.append(i > 0 ? ", " : "");
            sb.append(this.constantValues.get(i).type().llvmStr()).append(" ");
            sb.append(this.constantValues.get(i).llvmStr());
        }
        sb.append("]");
        return sb.toString();
    }
}
