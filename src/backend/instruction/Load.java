package backend.instruction;

import backend.oprand.*;
import backend.target.TargetBasicBlock;

import java.util.Objects;

public class Load extends TargetInstruction {
    private final SIZE size;
    private TargetRegister destination;
    private TargetAddress<?, ?> address;

    public enum SIZE {
        BYTE,
        WORD
    }

    public Load(TargetBasicBlock targetBasicBlock, SIZE size, TargetOperand destination, TargetOperand address) {
        super(targetBasicBlock);
        if (destination instanceof TargetRegister destinationRegister
                && address instanceof TargetAddress<?, ?> addressAddress) {
            this.size = size;
            this.destination = destinationRegister;
            this.address = addressAddress;
            addDef(this.destination);
            addUse(this.address);
        } else {
            throw new RuntimeException("When Load(), the type of destination or address is invalid. " +
                    "Got destination: " + destination + ", address: " + address);
        }
    }


    @Override
    public void replaceDefVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        if (Objects.equals(destination, virtualRegister)) {
            this.destination = physicalRegister;
        } else {
            throw new RuntimeException("When Load.replaceDefVirtualRegister(), the virtualRegister is not destination");
        }
    }

    @Override
    public void replaceUseVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        if (this.address.useRegisterSet().contains(virtualRegister)) {
            this.address = this.address.replaceUseVirtualRegister(physicalRegister, virtualRegister);
        } else {
            throw new RuntimeException("When Load().replaceUseVirtualRegister(), the virtualRegister is not in address");
        }
    }

    @Override
    public String mipsStr() {
        if (size == SIZE.BYTE) {
            return "lbu " + destination.mipsStr() + ", " + address.mipsStr();
        } else { // getBitWidth == SIZE.WORD
            return "lw " + destination.mipsStr() + ", " + address.mipsStr();
        }
    }
}
