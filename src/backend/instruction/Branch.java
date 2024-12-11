package backend.instruction;

import backend.oprand.*;
import backend.target.TargetBasicBlock;

import java.util.Objects;

public class Branch extends TargetInstruction {
    private TargetRegister cond;
    private final Label destination;
    private final boolean link;

    public Branch(TargetBasicBlock targetBasicBlock, TargetOperand destination, boolean link) {
        super(targetBasicBlock);
        if (destination instanceof Label destinationLabel) {
            this.cond = null;
            this.destination = destinationLabel;
            this.link = link;
            addUse(destinationLabel);
        } else {
            throw new RuntimeException("When Branch, the type of destination operand is not Label. Got" + destination);
        }
    }

    public Branch(TargetBasicBlock targetBasicBlock, TargetOperand cond, TargetOperand destination) {
        super(targetBasicBlock);
        if (cond instanceof TargetRegister condRegister && destination instanceof Label destinationLabel) {
            this.cond = condRegister;
            this.destination = destinationLabel;
            this.link = false;
            addUse(condRegister);
            addUse(destinationLabel);
        } else {
            throw new RuntimeException("When Branch(), the type of cond or destination is invalid. " +
                    "Got cond: " + cond + ", destination: " + destination);
        }
    }

    @Override
    public void replaceDefVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        throw new UnsupportedOperationException("When Branch.replaceDefVirtualRegister(), Branch should not have any defVirtualRegister");
    }

    @Override
    public void replaceUseVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        if (Objects.equals(cond, virtualRegister)) {
            this.cond = physicalRegister;
        } else {
            throw new RuntimeException("When Branch.replaceUseVirtualRegister(), the virtualRegister is not cond");
        }
    }

    @Override
    public String mipsStr() {
        if (this.cond != null) {
            return "bne $zero, " + this.cond.mipsStr() + ", " + this.destination.mipsStr();
        } else {
            if (this.link) {
                return "jal " + this.destination.mipsStr();
            } else {
                return "j " + this.destination.mipsStr();
            }
        }
    }
}
