package IR.type;

import java.util.Objects;

public record PointerType(
        IRType referenceType,
        // arrayDecay是为语义分析的输出准备的
        boolean arrayDecay
) implements IRType, IRType.VarSymbolType {
    @Override
    public String displayStr() {
        if (arrayDecay) {
            return referenceType.displayStr() + "Array";
        } else {
            throw new UnsupportedOperationException("A PointerType should not be display in SysY.");
        }
    }

    @Override
    public String llvmStr() {
        return this.referenceType.llvmStr() + "*";
    }

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        return this.llvmStr();
    }

    // WARNING 未重写hashCode方法，不得在Hash类容器中使用
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PointerType otherPointerType) {
            // 实际上没有什么ArrayDecay，所以不比较arrayDecay
            return Objects.equals(this.referenceType, otherPointerType.referenceType);
        } else {
            return false;
        }
    }
}
