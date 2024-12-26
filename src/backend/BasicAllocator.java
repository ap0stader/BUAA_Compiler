package backend;

import backend.instruction.Load;
import backend.instruction.Store;
import backend.instruction.TargetInstruction;
import backend.oprand.PhysicalRegister;
import backend.oprand.VirtualRegister;
import backend.target.TargetBasicBlock;
import backend.target.TargetFunction;
import backend.target.TargetModule;
import util.DoublyLinkedList;

import java.util.LinkedList;
import java.util.TreeSet;

public class BasicAllocator {
    private final TargetModule targetModule;
    private boolean finish = false;

    // 调度寄存器
    private final LinkedList<PhysicalRegister> dispatchRegisters;

    public BasicAllocator(TargetModule targetModule) {
        this.targetModule = targetModule;
        this.dispatchRegisters = new LinkedList<>();
        this.dispatchRegisters.add(PhysicalRegister.V0);
        this.dispatchRegisters.add(PhysicalRegister.V1);
    }

    private PhysicalRegister acquireDispatchPhysicalRegister() {
        PhysicalRegister resultRegister = this.dispatchRegisters.poll();
        if (resultRegister == null) {
            throw new RuntimeException("When acquireDispatchPhysicalRegister(), no more dispatch registers");
        }
        return resultRegister;
    }

    private void releaseDispatchPhysicalRegister(PhysicalRegister physicalRegister) {
        this.dispatchRegisters.push(physicalRegister);
    }

    public void allocRegister() {
        if (this.finish) {
            return;
        }
        this.targetModule.functions().forEach(this::allocFunction);
        this.finish = true;
    }

    private void allocFunction(TargetFunction targetFunction) {
        targetFunction.basicBlocks().forEach((node) -> this.allocBasicBlock(node.value()));
    }

    private void allocBasicBlock(TargetBasicBlock targetBasicBlock) {
        for (DoublyLinkedList.Node<TargetInstruction> instructionNode : targetBasicBlock.instructions()) {
            TargetInstruction instruction = instructionNode.value();
            TreeSet<PhysicalRegister> acquiredPhysicalRegisters = new TreeSet<>();
            for (VirtualRegister useVirtualRegister : instruction.useVirtualRegisterSet()) {
                if (!useVirtualRegister.isPreAllocated()) {
                    PhysicalRegister physicalRegister = this.acquireDispatchPhysicalRegister();
                    acquiredPhysicalRegisters.add(physicalRegister);
                    Load loadVirtualRegister = new Load(null, Load.SIZE.WORD,
                            physicalRegister, useVirtualRegister.address());
                    loadVirtualRegister.listNode().insertBefore(instructionNode);
                    instruction.replaceUseVirtualRegister(physicalRegister, useVirtualRegister);
                }
            }
            // 顺序申请，逆序释放
            acquiredPhysicalRegisters.descendingSet().forEach(this::releaseDispatchPhysicalRegister);
            acquiredPhysicalRegisters.clear();
            for (VirtualRegister defVirtualRegister : instruction.defVirtualRegisterSet()) {
                if (!defVirtualRegister.isPreAllocated()) {
                    PhysicalRegister physicalRegister = this.acquireDispatchPhysicalRegister();
                    acquiredPhysicalRegisters.add(physicalRegister);
                    instruction.replaceDefVirtualRegister(physicalRegister, defVirtualRegister);
                    Store storeVirtualRegister = new Store(null, Store.SIZE.WORD,
                            physicalRegister, defVirtualRegister.address());
                    storeVirtualRegister.listNode().insertAfter(instructionNode);
                }
            }
            // 顺序申请，逆序释放
            acquiredPhysicalRegisters.descendingSet().forEach(this::releaseDispatchPhysicalRegister);
            if (this.dispatchRegisters.size() != PhysicalRegister.DISPATCH_REGISTER_SIZE) {
                throw new IllegalStateException("When allocBasicBlock(), the size of dispatchRegisters is " + this.dispatchRegisters.size() +
                        " after the allocation of instruction " + instruction);
            }
        }
    }
}
