package backend.instruction;

import backend.oprand.Label;
import backend.oprand.PhysicalRegister;
import backend.oprand.TargetOperand;
import backend.oprand.VirtualRegister;
import backend.target.TargetBasicBlock;

public class Branch extends TargetInstruction {
    private final Label destination;

    public Branch(TargetBasicBlock targetBasicBlock, TargetOperand destination) {
        super(targetBasicBlock);
        if (destination instanceof Label destinationLabel) {
            this.destination = destinationLabel;
            addUse(destinationLabel);
        } else {
            throw new RuntimeException("When Branch, the type of destination operand is not Label. Got" + destination);
        }
    }


    @Override
    public void replaceDefVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        throw new UnsupportedOperationException("When Branch.replaceDefVirtualRegister(), Branch should not have any defVirtualRegister");
    }

    @Override
    public void replaceUseVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        throw new UnsupportedOperationException("When Branch.replaceUseVirtualRegister(), Branch should not have any useVirtualRegister");
    }

    @Override
    public String mipsStr() {
        return "j " + this.destination.mipsStr();
    }
}
