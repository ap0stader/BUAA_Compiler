package backend.oprand;

import java.util.Objects;
import java.util.Set;

public final class LabelBaseAddress extends TargetAddress<Label, LabelBaseAddress> {
    private final TargetRegister registerOffset;

    public LabelBaseAddress(Label label) {
        super(label);
        this.registerOffset = null;
    }

    private LabelBaseAddress(LabelBaseAddress oldAddress, TargetRegister registerOffset) {
        super(oldAddress);
        this.registerOffset = registerOffset;
    }

    private LabelBaseAddress(LabelBaseAddress oldAddress, ImmediateOffset immediateOffset) {
        super(oldAddress, immediateOffset);
        this.registerOffset = oldAddress.registerOffset;
    }

    public LabelBaseAddress addRegisterOffset(TargetRegister registerOffset) {
        if (this.registerOffset == null && this.immediateOffsetList.isEmpty()) {
            // 没有寄存器偏移和立即数偏移
            return new LabelBaseAddress(this, registerOffset);
        } else {
            throw new RuntimeException("When setRegisterOffset(), not both of registerOffset immediateOffset are null");
        }
    }

    @Override
    public LabelBaseAddress addImmediateOffset(ImmediateOffset immediateOffset) {
        if (registerOffset == null) {
            // 没有寄存器偏移
            return new LabelBaseAddress(this, immediateOffset);
        } else {
            throw new RuntimeException("When setImmediateOffset(), registerOffset is not null");
        }
    }

    @Override
    public Set<TargetRegister> useRegisterSet() {
        if (registerOffset != null) {
            return Set.of(registerOffset);
        } else {
            return Set.of();
        }
    }

    @Override
    public LabelBaseAddress replaceUseVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        if (Objects.equals(registerOffset, virtualRegister)) {
            return new LabelBaseAddress(this, physicalRegister);
        } else {
            throw new RuntimeException("When LabelBaseAddress.replaceUseVirtualRegister(), virtualRegister is not registerOffset");
        }
    }

    @Override
    public String mipsStr() {
        if (registerOffset != null) {
            return this.base.mipsStr() + "(" + registerOffset.mipsStr() + ")";
        } else if (!immediateOffsetList.isEmpty()) {
            return this.base.mipsStr() + "+" + this.immediateOffset().mipsStr();
        } else {
            return this.base.mipsStr();
        }
    }

    @Override
    public String toString() {
        return "LabelBaseAddress{" +
                "base=" + base +
                ", immediateOffsetList=" + immediateOffsetList +
                ", registerOffset=" + registerOffset +
                '}';
    }
}