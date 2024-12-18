package IR.type;

import frontend.visitor.symbol.SymbolType;

public class IntegerType
        implements IRType, SymbolType.Var, SymbolType.Const, SymbolType.Arg {
    // 整数类型，在SysY中整数类型有int和char两种该类型的特定实例，同时会使用i1作为icmp的返回结果类型，同时部分库函数需要使用到i64
    // 整数类型可以作为符号表中变量符号、常量符号、参数符号的登记类型
    // 整数类型的实例不能任意创建，需使用已经创建好的静态实例

    private final int numBits;

    static final IntegerType I1_INSTANCE = new IntegerType(1);

    private IntegerType(int numBits) {
        if (numBits < 0) {
            throw new IllegalArgumentException("When IntegerType(), got negative numBits");
        }
        this.numBits = numBits;
    }

    public int getBitWidth() {
        return numBits;
    }

    @Override
    public String displayStr() {
        throw new UnsupportedOperationException("A raw IntegerType should not be display in SysY.");
    }

    @Override
    public String llvmStr() {
        return "i" + this.numBits;
    }

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        return this.llvmStr();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntegerType otherIntegerType) {
            return this.numBits == otherIntegerType.numBits;
        } else {
            return false;
        }
    }

    static final class Int extends IntegerType {
        static final Int INSTANCE = new Int();

        private Int() {
            super(32);
        }

        @Override
        public String displayStr() {
            return "Int";
        }
    }

    static final class Char extends IntegerType {
        static final Char INSTANCE = new Char();

        private Char() {
            super(8);
        }

        @Override
        public String displayStr() {
            return "Char";
        }
    }
}
