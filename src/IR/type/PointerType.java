package IR.type;

import java.util.Objects;

public record PointerType(
        IRType referenceType,
        // arrayDecay是为语义分析的输出准备的，语义分析时要求参数为数组形式给出时仍然输出其为数组类型
        boolean arrayDecay
) implements IRType, IRType.ArgSymbolType {
    // 指针类型，在SysY中没有定义指针类型，但是在函数参数和左值转换中一维数组会退化为指针类型。同时全局变量，alloca的类型也是对应类型的指针类型
    // 指针类型可以作为符号表中参数符号的登记类型

    public PointerType(IRType referenceType) {
        this(referenceType, false);
    }

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
    // TODO 为避免在调用类型文本时遗漏了.llvmStr()，可以将此处的(array)输出去除
    @Override
    public String toString() {
        return this.llvmStr() + (this.arrayDecay ? "(array)" : "");
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
