package IR.value.constant;

import IR.type.IRType;
import IR.type.StructType;

import java.util.ArrayList;

public class ConstantStruct extends IRConstant<StructType> {
    private final ArrayList<IRConstant<?>> constantValues;

    public ConstantStruct(StructType structType, ArrayList<IRConstant<?>> constantValues) {
        super(structType);
        // 检查类型是否匹配
        if (structType.memberTypes().size() != constantValues.size()) {
            throw new RuntimeException("When ConstantStruct(), number of members mismatch. Got " + constantValues.size() +
                    ", expected " + structType.memberTypes().size());
        } else {
            for (int i = 0; i < structType.memberTypes().size(); i++) {
                if (!IRType.isEqual(structType.memberTypes().get(i), constantValues.get(i).type())) {
                    throw new RuntimeException("When ConstantStruct(), type mismatch. Got " + constantValues.get(i).type() +
                            " value " + constantValues.get(i) +
                            ", expected " + structType.memberTypes().get(i));
                }
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
        sb.append("<{ ");
        for (int i = 0; i < this.constantValues.size(); i++) {
            sb.append(i > 0 ? ", " : "");
            sb.append(this.constantValues.get(i).type().llvmStr()).append(" ");
            sb.append(this.constantValues.get(i).llvmStr());
        }
        sb.append(" }>");
        return sb.toString();
    }
}
