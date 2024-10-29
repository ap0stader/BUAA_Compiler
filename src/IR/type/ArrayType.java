package IR.type;

import frontend.visitor.symbol.SymbolType;

import java.util.Objects;

public record ArrayType(
        IRType elementType,
        int length
) implements IRType, SymbolType.Var, SymbolType.Const {
    // 数组类型，在SysY中只有一维数组，数组元素的类型只有IntegerType，并且只能为int和char
    // 数组类型可以作为符号表中变量符号和常量符号的登记类型

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
