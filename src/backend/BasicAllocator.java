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

import java.util.*;

public class BasicAllocator {
    private final TargetModule targetModule;
    private boolean finish = false;
    private final LinkedList<PhysicalRegister> tempRegisters;

    public BasicAllocator(TargetModule targetModule) {
        this.targetModule = targetModule;
        this.tempRegisters = new LinkedList<>();
    }

    private void initRegisters() {
        // 临时寄存器
        tempRegisters.add(PhysicalRegister.T0);
        tempRegisters.add(PhysicalRegister.T1);
        tempRegisters.add(PhysicalRegister.T2);
        tempRegisters.add(PhysicalRegister.T3);
        tempRegisters.add(PhysicalRegister.T4);
        tempRegisters.add(PhysicalRegister.T5);
        tempRegisters.add(PhysicalRegister.T6);
        tempRegisters.add(PhysicalRegister.T7);
        tempRegisters.add(PhysicalRegister.T8);
        tempRegisters.add(PhysicalRegister.T9);
        tempRegisters.add(PhysicalRegister.T10);
    }

    public void allocRegister() {
        if (this.finish) {
            return;
        }
        targetModule.functions().forEach(this::allocFunction);
        this.finish = true;
    }

    private void allocFunction(TargetFunction targetFunction) {
        // 对每一个函数，寄存器的占用状态要重置
        this.initRegisters();
        targetFunction.basicBlocks().forEach((node) -> this.allocBasicBlock(node.value()));
    }

    private PhysicalRegister acquireTempPhysicalRegister(TargetFunction targetFunction) {
        PhysicalRegister resultRegister = tempRegisters.poll();
        targetFunction.stackFrame.ensureSaveRegister(resultRegister);
        return resultRegister;
    }

    private void releaseTempPhysicalRegister(PhysicalRegister physicalRegister) {
        tempRegisters.push(physicalRegister);
    }

    private void allocBasicBlock(TargetBasicBlock targetBasicBlock) {
        TargetFunction targetFunction = targetBasicBlock.parent();
        for (DoublyLinkedList.Node<TargetInstruction> instructionNode : targetBasicBlock.instructions()) {
            TargetInstruction instruction = instructionNode.value();
            TreeSet<PhysicalRegister> acquiredPhysicalRegisters = new TreeSet<>();
            for (VirtualRegister useVirtualRegister : instruction.useVirtualRegisterSet()) {
                PhysicalRegister physicalRegister = acquireTempPhysicalRegister(targetFunction);
                acquiredPhysicalRegisters.add(physicalRegister);
                Load loadVirtualRegister = new Load(null, Load.SIZE.WORD,
                        physicalRegister, useVirtualRegister.address());
                loadVirtualRegister.listNode().insertBefore(instructionNode);
                instruction.replaceUseVirtualRegister(physicalRegister, useVirtualRegister);
            }
            // 顺序申请，逆序释放
            acquiredPhysicalRegisters.descendingSet().forEach(this::releaseTempPhysicalRegister);
            acquiredPhysicalRegisters.clear();
            for (VirtualRegister defVirtualRegister : instruction.defVirtualRegisterSet()) {
                PhysicalRegister physicalRegister = acquireTempPhysicalRegister(targetFunction);
                acquiredPhysicalRegisters.add(physicalRegister);
                instruction.replaceDefVirtualRegister(physicalRegister, defVirtualRegister);
                Store storeVirtualRegister = new Store(null, Store.SIZE.WORD,
                        physicalRegister, defVirtualRegister.address());
                storeVirtualRegister.listNode().insertAfter(instructionNode);
            }
            // 顺序申请，逆序释放
            acquiredPhysicalRegisters.descendingSet().forEach(this::releaseTempPhysicalRegister);
        }
    }
}
