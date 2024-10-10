package IR.type;

public final class ArrayType implements Type {
    private final Type elementType;
    private final int length;

    public ArrayType(Type elementType, int length) {
        this.elementType = elementType;
        this.length = length;
    }

    public Type elementType() {
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
