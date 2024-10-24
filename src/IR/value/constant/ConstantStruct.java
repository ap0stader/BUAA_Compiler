package IR.value.constant;

import IR.type.StructType;

import java.util.ArrayList;
import java.util.Objects;

public class ConstantStruct extends Constant {
    private final ArrayList<Constant> constantValues;

    public ConstantStruct(StructType structType, ArrayList<Constant> constantValues) {
        super(structType);
        if (structType.memberTypes().size() != constantValues.size()) {
            throw new RuntimeException("When ConstantStruct(), number of members mismatch. Got " + constantValues.size() +
                    ", expected " + structType.memberTypes().size());
        } else {
            for (int i = 0; i < structType.memberTypes().size(); i++) {
                if (!Objects.equals(structType.memberTypes().get(i), constantValues.get(i).type())) {
                    throw new RuntimeException("When ConstantStruct(), type mismatch. Got " + constantValues.get(i).type() +
                            ", expected " + structType.memberTypes().get(i));
                }
            }
        }
        this.constantValues = constantValues;
    }


    @Override
    public String llvmStr() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.type.llvmStr());
        sb.append(" <{ ");
        if (!this.constantValues.isEmpty()) {
            sb.append(this.constantValues.get(0).llvmStr());
            for (int i = 1; i < this.constantValues.size(); i++) {
                sb.append(", ");
                sb.append(this.constantValues.get(i).llvmStr());
            }
        }
        sb.append(" }>");
        return sb.toString();
    }
}
