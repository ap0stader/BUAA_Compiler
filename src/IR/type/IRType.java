package IR.type;

import java.util.Objects;

public interface IRType {
    String displayStr();

    String llvmStr();

    static boolean isEqual(IRType type1, IRType type2) {
        return Objects.equals(type1, type2);
    }

    interface VarSymbolType extends IRType {}

    interface ConstSymbolType extends IRType {}

    interface FuncSymbolType extends IRType {}
}
