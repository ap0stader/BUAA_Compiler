package backend.oprand;

public final class RegisterBaseAddress extends TargetAddress<PhysicalRegister, RegisterBaseAddress> {
    public RegisterBaseAddress(PhysicalRegister base, ImmediateOffset immediateOffset) {
        super(base, immediateOffset);
    }

    private RegisterBaseAddress(RegisterBaseAddress oldAddress, ImmediateOffset immediateOffset) {
        super(oldAddress, immediateOffset);
    }

    @Override
    public RegisterBaseAddress addImmediateOffset(ImmediateOffset immediateOffset) {
        return new RegisterBaseAddress(this, immediateOffset);
    }

    @Override
    public String mipsStr() {
        return this.immediateOffset().mipsStr() + "(" + this.base.mipsStr() + ")";
    }

    @Override
    public String toString() {
        return "RegisterBaseAddress{" +
                "base=" + base +
                ", immediateOffsetList=" + immediateOffsetList +
                '}';
    }
}