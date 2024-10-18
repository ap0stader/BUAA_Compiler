package IR.type;

public final class ArrayType implements IRType {
    private final IRType elementType;
    private final int length;

    public ArrayType(IRType elementType, int length) {
        this.elementType = elementType;
        this.length = length;
    }

    public IRType elementType() {
        return this.elementType;
    }

    public int length() {
        return this.length;
    }

    @Override
    public String displayStr() {
        return this.elementType.displayStr() + "Array";
    }
}
