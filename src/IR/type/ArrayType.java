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

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        return this.llvmStr();
    }

    // WARNING 未重写hashCode方法，不得在Hash类容器中使用
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
