package backend.oprand;

public record Immediate(Integer value) implements TargetOperand, TargetAddress.ImmediateOffset {
    public static final Immediate ZERO = new Immediate(0);

    public Immediate add(Immediate other) {
        return new Immediate(value + other.value);
    }

    @Override
    public Immediate calc() {
        return this;
    }

    @Override
    public String mipsStr() {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }
}
