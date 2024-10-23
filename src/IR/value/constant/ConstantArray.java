package IR.value.constant;

import IR.type.ArrayType;

import java.util.ArrayList;
import java.util.Objects;

public class ConstantArray extends Constant {
    private final ArrayList<Constant> constantValues;

    public ConstantArray(ArrayType arrayType, ArrayList<Constant> constantValues) {
        super(arrayType);
        for (Constant value : constantValues) {
            if (!Objects.equals(arrayType.elementType(), value.type())) {
                throw new RuntimeException("When ConstantArray(), type mismatch. Got " + value.type() +
                        ", expected " + arrayType.elementType());
            }
        }
        this.constantValues = constantValues;
    }

    @Override
    public String llvmStr() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.type.llvmStr());
        sb.append(" [");
        if (!this.constantValues.isEmpty()) {
            sb.append(this.constantValues.get(0).llvmStr());
            for (int i = 1; i < this.constantValues.size(); i++) {
                sb.append(", ");
                sb.append(this.constantValues.get(i).llvmStr());
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
