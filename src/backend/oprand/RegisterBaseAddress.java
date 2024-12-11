package backend.oprand;

import java.util.Objects;
import java.util.Set;

public final class RegisterBaseAddress extends TargetAddress<TargetRegister, RegisterBaseAddress> {
    public RegisterBaseAddress(TargetRegister base, ImmediateOffset immediateOffset) {
        super(base, immediateOffset);
    }

    private RegisterBaseAddress(RegisterBaseAddress oldAddress, ImmediateOffset immediateOffset) {
        super(oldAddress, immediateOffset);
    }

    private RegisterBaseAddress(RegisterBaseAddress oldAddress, TargetRegister base) {
        super(oldAddress, base);
    }

    public RegisterBaseAddress replaceBaseRegister(TargetRegister newBase) {
        return new RegisterBaseAddress(this, newBase);
    }

    @Override
    public RegisterBaseAddress addImmediateOffset(ImmediateOffset immediateOffset) {
        return new RegisterBaseAddress(this, immediateOffset);
    }

    @Override
    public Set<TargetRegister> useRegisterSet() {
        return Set.of(base);
    }

    @Override
    public RegisterBaseAddress replaceUseVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        if (Objects.equals(base, virtualRegister)) {
            return new RegisterBaseAddress(this, physicalRegister);
        } else {
            throw new RuntimeException("When RegisterBaseAddress.replaceUseVirtualRegister, virtualRegister is not base");
        }
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