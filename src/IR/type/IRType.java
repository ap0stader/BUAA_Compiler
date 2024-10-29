package IR.type;

import java.util.Objects;

public interface IRType {
    // 语义分析时输出的字符串
    String displayStr();

    // LLVM IR输出的字符串
    String llvmStr();

    // 获得不能任意创建的类型创建好的静态实例
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

    // 两种类型是否相等
    static boolean isEqual(IRType type1, IRType type2) {
        return Objects.equals(type1, type2);
    }

    // 变量符号的登记类型
    interface VarSymbolType extends IRType {
    }

    // 常量符号的登记类型
    interface ConstSymbolType extends IRType {
    }

    // 参数符号的登记类型
    interface ArgSymbolType extends IRType {
    }

    // 函数符号的登记类型直接使用FunctionType
}
