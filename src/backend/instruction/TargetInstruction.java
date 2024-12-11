package backend.instruction;

import backend.oprand.*;
import backend.target.TargetBasicBlock;
import util.DoublyLinkedList;

import java.util.Set;
import java.util.TreeSet;

public abstract class TargetInstruction {
    private final DoublyLinkedList.Node<TargetInstruction> listNode;
    private final Set<VirtualRegister> defVirtualRegisterSet;
    private final Set<VirtualRegister> useVirtualRegisterSet;

    public TargetInstruction(TargetBasicBlock targetBasicBlock) {
        this.listNode = new DoublyLinkedList.Node<>(this);
        if (targetBasicBlock != null) {
            targetBasicBlock.appendInstruction(this);
        }
        this.defVirtualRegisterSet = new TreeSet<>();
        this.useVirtualRegisterSet = new TreeSet<>();
    }

    public DoublyLinkedList.Node<TargetInstruction> listNode() {
        return listNode;
    }

    public Set<VirtualRegister> defVirtualRegisterSet() {
        return defVirtualRegisterSet;
    }

    public Set<VirtualRegister> useVirtualRegisterSet() {
        return useVirtualRegisterSet;
    }

    protected void addDef(TargetOperand defOperand) {
        if (defOperand instanceof VirtualRegister defVirtualRegister) {
            this.defVirtualRegisterSet.add(defVirtualRegister);
        }
    }

    protected void addUse(TargetOperand useOperand) {
        if (useOperand instanceof VirtualRegister useVirtualRegister) {
            this.useVirtualRegisterSet.add(useVirtualRegister);
        } else if (useOperand instanceof TargetAddress<?, ?> useTargetAddress) {
            useTargetAddress.useRegisterSet().forEach(this::addUse);
        }
    }

    public abstract void replaceDefVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister);

    public abstract void replaceUseVirtualRegister(PhysicalRegister physicalRegister, VirtualRegister virtualRegister);

    public abstract String mipsStr();

    @Override
    public String toString() {
        return this.mipsStr();
    }
}
