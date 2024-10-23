package IR.type;

import java.util.Objects;

public record ArrayType(
        IRType elementType,
        int length
) implements IRType, IRType.VarSymbolType, IRType.ConstSymbolType {
    @Override
    public String displayStr() {
        return this.elementType.displayStr() + "Array";
    }

    @Override
    public String llvmStr() {
        return "[" + this.length + " x " + this.elementType.llvmStr() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArrayType otherArrayType) {
            return Objects.equals(this.elementType, otherArrayType.elementType)
                    && this.length == otherArrayType.length;
        } else {
            return false;
        }
    }
}
