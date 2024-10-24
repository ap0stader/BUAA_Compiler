package IR.type;

public abstract class IntegerType implements IRType, IRType.VarSymbolType, IRType.ConstSymbolType {
    private final int size;

    private IntegerType(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }

    @Override
    public String llvmStr() {
        return "i" + this.size;
    }

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        return this.llvmStr();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IntegerType otherIntegerType) {
            return this.size == otherIntegerType.size;
        } else {
            return false;
        }
    }

    public static final class Int extends IntegerType {
        static final Int INSTANCE = new Int();

        private Int() {
            super(32);
        }

        @Override
        public String displayStr() {
            return "Int";
        }
    }

    public static final class Char extends IntegerType {
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
