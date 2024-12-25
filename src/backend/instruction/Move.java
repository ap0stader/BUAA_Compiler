package backend.instruction;

import backend.oprand.*;
import backend.target.TargetBasicBlock;

import java.util.Objects;

public class Move extends TargetInstruction {
    private TargetRegister destination;
    private TargetOperand origin;

    public Move(TargetBasicBlock targetBasicBlock, TargetOperand destination, TargetOperand origin) {
        super(targetBasicBlock);
        if (destination instanceof TargetRegister destinationRegister) {
            this.destination = destinationRegister;
            this.origin = origin;
            addDef(this.destination);
            addUse(this.origin);
        } else {
            throw new RuntimeException("When Move(), the type of destination operand is not TargetRegister. Got " + destination);
        }
    }

    @Override
    public void replaceDefVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        if (Objects.equals(destination, virtualRegister)) {
            this.destination = physicalRegister;
        }
    }

    @Override
    public void replaceUseVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        if (Objects.equals(origin, virtualRegister)) {
            this.origin = physicalRegister;
        }
        if (origin instanceof TargetAddress<?, ?> addressOrigin
                && addressOrigin.useRegisterSet().contains(virtualRegister)) {
            this.origin = addressOrigin.replaceUseVirtualRegister(physicalRegister, virtualRegister);
        }
    }

    @Override
    public String mipsStr() {
        if (origin instanceof TargetRegister targetRegister) {
            return "move " + destination.mipsStr() + ", " + targetRegister.mipsStr();
        } else if (origin instanceof Immediate immediate) {
            return "li " + destination.mipsStr() + ", " + immediate.mipsStr();
        } else if (origin instanceof Label || origin instanceof TargetAddress<?, ?>) {
            return "la " + destination.mipsStr() + ", " + origin.mipsStr();
        } else {
            throw new RuntimeException("When Move.destination(), the type of origin operand is invalid. Got " + origin);
        }
    }
}
