package IR.type;

public final class PointerType implements IRType {
    private final boolean arrayDecay;
    private final IRType referenceType;

    public PointerType(IRType referenceType) {
        this.arrayDecay = true;
        this.referenceType = referenceType;
    }

    public IRType referenceType() {
        return referenceType;
    }

    @Override
    public String displayStr() {
        if (arrayDecay) {
            return referenceType.displayStr() + "Array";
        } else {
            throw new UnsupportedOperationException("A PointerType should not be display in SysY.");
        }
    }
}
