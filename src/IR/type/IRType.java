package IR.type;

import java.util.Objects;

public interface IRType {
    String displayStr();

    String llvmStr();

    static VoidType getVoidTy() {
        return VoidType.INSTANCE;
    }

    static LabelType getLabelTy() {
        return LabelType.INSTANCE;
    }

    static IntegerType getInt1Ty() {
        return IntegerType.I1_INSTANCE;
    }

    static IntegerType getInt8Ty() {
        return IntegerType.Char.INSTANCE;
    }

    static IntegerType getInt32Ty() {
        return IntegerType.Int.INSTANCE;
    }

    static IntegerType getInt64Ty() {
        return IntegerType.I64_INSTANCE;
    }

    static boolean isEqual(IRType type1, IRType type2) {
        return Objects.equals(type1, type2);
    }

    interface VarSymbolType extends IRType {
    }

    interface ConstSymbolType extends IRType {
    }
}
