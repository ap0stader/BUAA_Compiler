package frontend.visitor.symbol;

import IR.type.IRType;

public interface SymbolType extends IRType {
    // 变量符号的登记类型
    interface Var extends SymbolType {
    }

    // 常量符号的登记类型
    interface Const extends SymbolType {
    }

    // 参数符号的登记类型
    interface Arg extends SymbolType {
    }

    // 函数符号的登记类型直接使用FunctionType
}