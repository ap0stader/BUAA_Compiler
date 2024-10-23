package IR.type;

import java.util.ArrayList;
import java.util.Objects;

public record FunctionType(
        IRType returnType,
        ArrayList<IRType> parametersType
) implements IRType, IRType.FuncSymbolType {
    @Override
    public String displayStr() {
        return returnType.displayStr() + "Func";
    }

    @Override
    public String llvmStr() {
        throw new UnsupportedOperationException("Can not call llvmStr() of FunctionType directly");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FunctionType otherFunctionType) {
            return Objects.equals(this.returnType, otherFunctionType.returnType)
                    && Objects.equals(this.parametersType, otherFunctionType.parametersType);
        } else {
            return false;
        }
    }
}
