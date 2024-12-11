package backend.instruction;

import backend.oprand.PhysicalRegister;
import backend.oprand.VirtualRegister;
import backend.target.TargetBasicBlock;

public class Syscall extends TargetInstruction {
    private final int code;

    public Syscall(TargetBasicBlock targetBasicBlock, int code) {
        super(targetBasicBlock);
        this.code = code;
    }

    @Override
    public void replaceDefVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        throw new UnsupportedOperationException("When Syscall.replaceDefVirtualRegister(), Syscall should not have any defVirtualRegister");
    }

    @Override
    public void replaceUseVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister) {
        throw new UnsupportedOperationException("When Syscall.replaceDefVirtualRegister(), Syscall should not have any defVirtualRegister");
    }

    @Override
    public String mipsStr() {
        return "li $v0, " + this.code + "\n\t" +
                "syscall";
    }
}
