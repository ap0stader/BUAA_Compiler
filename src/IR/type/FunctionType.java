package IR.type;

import java.util.ArrayList;

public final class FunctionType implements Type {
    private final Type returnType;
    private final ArrayList<Type> parametersType;

    public FunctionType(Type returnType, ArrayList<Type> parametersType) {
        this.returnType = returnType;
        this.parametersType = parametersType;
    }

    public Type returnType() {
        return returnType;
    }

    public ArrayList<Type> parametersType() {
        // TODO 移除该处不必要的保护
        return new ArrayList<>(parametersType);
    }

    @Override
    public String displayStr() {
        return returnType.displayStr() + "Func";
    }
}
