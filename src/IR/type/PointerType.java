package IR.type;

public final class PointerType implements Type {
    private final boolean arrayDecay;
    private final Type referenceType;

    public PointerType(Type referenceType) {
        this.arrayDecay = true;
        this.referenceType = referenceType;
    }

    public Type referenceType() {
        return referenceType;
    }

    @Override
    public String displayStr() {
        if (arrayDecay) {
            return referenceType.displayStr() + "Array";
        } else {
            return referenceType.displayStr() + "Pointer";
        }
    }
}
