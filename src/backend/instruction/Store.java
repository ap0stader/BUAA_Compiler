package backend.instruction;

import backend.oprand.*;
import backend.target.TargetBasicBlock;

import java.util.Objects;

public class Store extends TargetInstruction {
    private final SIZE size;
    private TargetRegister origin;
    private TargetAddress<?, ?> address;

    public enum SIZE {
        BYTE,
        WORD
    }

    public Store(TargetBasicBlock targetBasicBlock, SIZE size, TargetOperand origin, TargetOperand address) {
        super(targetBasicBlock);
        if (origin instanceof TargetRegister originRegister
                && address instanceof TargetAddress<?, ?> addressAddress) {
            this.size = size;
            this.origin = originRegister;
            this.address = addressAddress;
            addUse(this.origin);
            addUse(this.address);
        } else {
            throw new RuntimeException("When Store(), the type of origin or address is invalid. " +
                    "Got origin: " + origin + ", address: " + address);
        }
    }

    @Override
    public void replaceDefVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        throw new UnsupportedOperationException("When Store.replaceDefVirtualRegister(), Store should not have any defVirtualRegister");
    }

    @Override
    public void replaceUseVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        if (Objects.equals(origin, virtualRegister)) {
            this.origin = physicalRegister;
        }
        if (this.address.useRegisterSet().contains(virtualRegister)) {
            this.address = this.address.replaceUseVirtualRegister(physicalRegister, virtualRegister);
        }
    }

    @Override
    public String mipsStr() {
        if (size == SIZE.BYTE) {
            return "sb " + origin.mipsStr() + ", " + address.mipsStr();
        } else { // size == SIZE.WORD
            return "sw " + origin.mipsStr() + ", " + address.mipsStr();
        }
    }
}
