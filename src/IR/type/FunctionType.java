package IR.type;

import java.util.ArrayList;

public final class FunctionType implements IRType, IRType.FuncSymbolType {
    private final IRType returnType;
    private final ArrayList<IRType> parametersType;

    public FunctionType(IRType returnType, ArrayList<IRType> parametersType) {
        this.returnType = returnType;
        this.parametersType = parametersType;
    }

    public IRType returnType() {
        return returnType;
    }

    public ArrayList<IRType> parametersType() {
        // TODO 移除该处不必要的保护
        return new ArrayList<>(parametersType);
    }

    @Override
    public String displayStr() {
        return returnType.displayStr() + "Func";
    }
}
