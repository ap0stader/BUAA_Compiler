package IR.type;

public abstract class IntegerType implements IRType, IRType.VarSymbolType, IRType.ConstSymbolType {
    private final int size;

    private IntegerType(int size) {
        this.size = size;
    }

    public int size() {
        return size;
    }

    public static final class Int extends IntegerType {
        public Int() {
            super(32);
        }

        @Override
        public String displayStr() {
            return "Int";
        }
    }

    public static final class Char extends IntegerType {
        public Char() {
            super(8);
        }

        @Override
        public String displayStr() {
            return "Char";
        }
    }
}