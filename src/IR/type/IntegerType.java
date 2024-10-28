package IR.type;

public class IntegerType implements IRType, IRType.VarSymbolType, IRType.ConstSymbolType {
    private final int size;

    static final IntegerType I1_INSTANCE = new IntegerType(1);
    static final IntegerType I64_INSTANCE = new IntegerType(64);

    private IntegerType(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("When IntegerType(), got negative size");
        }
        this.size = size;
    }

    public int size() {
        return size;
    }

    @Override
    public String displayStr() {
        throw new UnsupportedOperationException("A raw Integer should not be display in SysY.");
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
