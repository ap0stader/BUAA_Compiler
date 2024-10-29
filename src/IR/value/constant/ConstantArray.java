package IR.value.constant;

import IR.type.ArrayType;

import java.util.ArrayList;
import java.util.Objects;

public class ConstantArray extends Constant<ArrayType> {
    private final ArrayList<Constant<?>> constantValues;

    public ConstantArray(ArrayType arrayType, ArrayList<Constant<?>> constantValues) {
        super(arrayType);
        // 检查类型是否匹配
        for (Constant<?> value : constantValues) {
            if (!Objects.equals(arrayType.elementType(), value.type())) {
                throw new RuntimeException("When ConstantArray(), type mismatch. Got " + value.type() + " value " + value +
                        ", expected " + arrayType.elementType());
            }
        }
        this.constantValues = constantValues;
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
