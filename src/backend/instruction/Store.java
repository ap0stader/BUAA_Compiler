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

    public Store(TargetBasicBlock targetBasicBlock, int size, TargetOperand origin, TargetOperand address) {
        super(targetBasicBlock);
        if (origin instanceof TargetRegister originRegister
                && address instanceof TargetAddress<?, ?> addressAddress) {
            if (size == 8) {
                this.size = SIZE.BYTE;
            } else if (size == 32) {
                this.size = SIZE.WORD;
            } else {
                throw new RuntimeException("When Store(), the size is invalid. Got " + size + ", expected 8 or 32");
            }
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
        } else if (this.address.useRegisterSet().contains(virtualRegister)) {
            this.address = this.address.replaceUseVirtualRegister(physicalRegister, virtualRegister);
        } else {
            throw new RuntimeException("When Store().replaceUseVirtualRegister(), the replaceUseVirtualRegister is not origin and not in address");
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
