package IR.type;

public final class PointerType implements IRType, IRType.VarSymbolType {
    private final IRType referenceType;
    private final boolean arrayDecay;

    public PointerType(IRType referenceType, boolean arrayDecay) {
        this.referenceType = referenceType;
        this.arrayDecay = arrayDecay;
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
