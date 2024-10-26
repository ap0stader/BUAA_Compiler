package IR.type;

import java.util.ArrayList;
import java.util.Objects;

public record FunctionType(
        IRType returnType,
        ArrayList<IRType> parametersType
) implements IRType {
    @Override
    public String displayStr() {
        return returnType.displayStr() + "Func";
    }

    @Override
    public String llvmStr() {
        throw new UnsupportedOperationException("Can not call llvmStr() of FunctionType directly");
    }

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("define dso_local ");
        sb.append(returnType.llvmStr());
        sb.append(" @(");
        for (int i = 0; i < parametersType.size(); i++) {
            sb.append(i > 0 ? ", " : "");
            sb.append(parametersType.get(i).llvmStr());
            sb.append(" %").append(i);
        }
        sb.append(")");
        return sb.toString();
    }

    // WARNING 未重写hashCode方法，不得在Hash类容器中使用
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FunctionType otherFunctionType) {
            return Objects.equals(this.returnType, otherFunctionType.returnType)
                    // ArrayList的比较方法自动严格比较
                    && Objects.equals(this.parametersType, otherFunctionType.parametersType);
        } else {
            return false;
        }
    }
}
