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

public class LinearScanAllocator {
    private final TargetModule targetModule;
    private boolean finish = false;

    // 临时寄存器
    private final LinkedList<PhysicalRegister> tempRegisters;
    // 保存变量寄存器
    private final LinkedList<PhysicalRegister> savedRegisters;
    // 保存一个函数中的虚拟寄存器的访问情况
    private final HashMap<VirtualRegister, ArrayList<DoublyLinkedList.Node<TargetInstruction>>> virtualRegisterAccessPlaces;
    // 保存一个函数中的虚拟寄存器是否跨越了基本块，true为跨越了基本块，false为未跨越基本块
    private final HashMap<VirtualRegister, Boolean> virtualRegisterCrossBasicBlock;
    // 保存一个函数中的虚拟寄存器和物理寄存器的对应关系
    private final HashMap<VirtualRegister, PhysicalRegister> virtualRegisterAllocResult;
    // 保存一个函数中一条指令Use的虚拟寄存器使用的调度寄存器
    private final HashMap<DoublyLinkedList.Node<TargetInstruction>, HashSet<PhysicalRegister>> instructionUseUsedDispatchRegisters;

    public LinearScanAllocator(TargetModule targetModule) {
        this.targetModule = targetModule;

        this.tempRegisters = new LinkedList<>();
        this.tempRegisters.add(PhysicalRegister.T0);
        this.tempRegisters.add(PhysicalRegister.T1);
        this.tempRegisters.add(PhysicalRegister.T2);
        this.tempRegisters.add(PhysicalRegister.T3);
        this.tempRegisters.add(PhysicalRegister.T4);
        this.tempRegisters.add(PhysicalRegister.T5);
        this.tempRegisters.add(PhysicalRegister.T6);
        this.tempRegisters.add(PhysicalRegister.T7);
        this.tempRegisters.add(PhysicalRegister.T8);
        this.tempRegisters.add(PhysicalRegister.T9);

        this.savedRegisters = new LinkedList<>();

        this.virtualRegisterAccessPlaces = new HashMap<>();
        this.virtualRegisterCrossBasicBlock = new HashMap<>();
        this.virtualRegisterAllocResult = new HashMap<>();
        this.instructionUseUsedDispatchRegisters = new HashMap<>();
    }

    private void initRegisters() {
        this.savedRegisters.clear();
        this.savedRegisters.add(PhysicalRegister.S0);
        this.savedRegisters.add(PhysicalRegister.S1);
        this.savedRegisters.add(PhysicalRegister.S2);
        this.savedRegisters.add(PhysicalRegister.S3);
        this.savedRegisters.add(PhysicalRegister.S4);
        this.savedRegisters.add(PhysicalRegister.S5);
        this.savedRegisters.add(PhysicalRegister.S6);
        this.savedRegisters.add(PhysicalRegister.S7);
        this.savedRegisters.add(PhysicalRegister.S8);
        this.savedRegisters.add(PhysicalRegister.S9);
        this.savedRegisters.add(PhysicalRegister.S10);
        this.savedRegisters.add(PhysicalRegister.S11);
    }

    private PhysicalRegister acquireUseDispatchRegister(DoublyLinkedList.Node<TargetInstruction> targetInstructionNode) {
        HashSet<PhysicalRegister> used = this.instructionUseUsedDispatchRegisters.get(targetInstructionNode);
        if (!used.contains(PhysicalRegister.V0)) {
            used.add(PhysicalRegister.V0);
            return PhysicalRegister.V0;
        } else if (!used.contains(PhysicalRegister.V1)) {
            used.add(PhysicalRegister.V1);
            return PhysicalRegister.V1;
        } else {
            throw new RuntimeException("When acquireUseDispatchRegister(), no more dispatch registers");
        }
    }

    public void allocRegister() {
        if (this.finish) {
            return;
        }
        this.targetModule.functions().forEach(this::allocFunction);
        this.finish = true;
    }

    private void allocFunction(TargetFunction targetFunction) {
        // 对每一个函数，寄存器的占用状态要重置
        this.initRegisters();
        this.virtualRegisterAccessPlaces.clear();
        this.virtualRegisterCrossBasicBlock.clear();
        this.virtualRegisterAllocResult.clear();
        this.instructionUseUsedDispatchRegisters.clear();
        // 第一步：按照基本块进行划分，区分跨基本块和不跨基本块的寄存器
        this.generateAccessPlaceInfo(targetFunction);
        // 第二步：分配临时寄存器
        targetFunction.basicBlocks().forEach(basicBlockNode -> this.allocTempRegister(basicBlockNode.value()));
        // 第三步：分配全局寄存器

    }

    private void generateAccessPlaceInfo(TargetFunction targetFunction) {
        for (DoublyLinkedList.Node<TargetBasicBlock> basicBlockNode : targetFunction.basicBlocks()) {
            for (DoublyLinkedList.Node<TargetInstruction> instructionNode : basicBlockNode.value().instructions()) {
                instructionNode.value().defVirtualRegisterSet()
                        .forEach(virtualRegister -> this.addAccessPlace(virtualRegister, instructionNode));
                instructionNode.value().useVirtualRegisterSet()
                        .forEach(virtualRegister -> this.addAccessPlace(virtualRegister, instructionNode));
                // 遍历同时初始化调度寄存器使用表
                this.instructionUseUsedDispatchRegisters.put(instructionNode, new HashSet<>());
            }
        }
        // 参数的虚拟寄存器被设置了preDefined，认为preDefined是一次访问，所以不属于局部寄存器应当参与分配的范围
        this.virtualRegisterAccessPlaces.forEach((virtualRegister, accessPlaces) -> {
            this.virtualRegisterCrossBasicBlock.put(virtualRegister,
                    virtualRegister.preDefined() || accessPlaces.stream().map(DoublyLinkedList.Node::parent).distinct().count() > 1);
        });
    }

    private void addAccessPlace(VirtualRegister virtualRegister, DoublyLinkedList.Node<TargetInstruction> instructionNode) {
        if (!this.virtualRegisterAccessPlaces.containsKey(virtualRegister)) {
            this.virtualRegisterAccessPlaces.put(virtualRegister, new ArrayList<>());
        }
        this.virtualRegisterAccessPlaces.get(virtualRegister).add(instructionNode);
    }

    private void allocTempRegister(TargetBasicBlock targetBasicBlock) {
        for (DoublyLinkedList.Node<TargetInstruction> instructionNode : targetBasicBlock.instructions()) {
            TargetInstruction instruction = instructionNode.value();
            // 对于不跨越基本块的临时虚拟寄存器，都是先定义后进行使用的
            for (VirtualRegister useVirtualRegister : instruction.useVirtualRegisterSet()) {
                if (!this.virtualRegisterCrossBasicBlock.get(useVirtualRegister)) {
                    // 查询替换
                    if (this.virtualRegisterAllocResult.containsKey(useVirtualRegister)) {
                        // 在此之前该临时虚拟寄存器成功分配到临时寄存器，使用之前分配的临时寄存器
                        instruction.replaceUseVirtualRegister(this.virtualRegisterAllocResult.get(useVirtualRegister), useVirtualRegister);
                    } else {
                        // 在此之前该临时虚拟寄存器未成功分配到临时寄存器，分配调度寄存器，从内存当中加载
                        PhysicalRegister physicalRegister = this.acquireUseDispatchRegister(instructionNode);
                        Load loadVirtualRegister = new Load(null, Load.SIZE.WORD,
                                physicalRegister, useVirtualRegister.address());
                        loadVirtualRegister.listNode().insertBefore(instructionNode);
                        instruction.replaceUseVirtualRegister(physicalRegister, useVirtualRegister);
                    }
                    // 尝试释放
                    if (this.virtualRegisterAllocResult.containsKey(useVirtualRegister)) {
                        // 如果这是临时虚拟寄存器最后一次使用，那么释放其持有的临时寄存器
                        // 因为基本块内的执行只能是顺序的，所以使用位置列表的最后一个记录就是最后一次使用位置
                        if (this.virtualRegisterAccessPlaces.get(useVirtualRegister).indexOf(instructionNode)
                                == this.virtualRegisterAccessPlaces.get(useVirtualRegister).size() - 1) {
                            this.releaseTempPhysicalRegister(this.virtualRegisterAllocResult.get(useVirtualRegister));
                        }
                    }
                }
            }
            for (VirtualRegister defVirtualRegister : instruction.defVirtualRegisterSet()) {
                if (!this.virtualRegisterCrossBasicBlock.get(defVirtualRegister)) {
                    // 查询替换
                    if (this.virtualRegisterAllocResult.containsKey(defVirtualRegister)) {
                        // 在此之前该临时虚拟寄存器成功分配到临时寄存器，使用之前分配的临时寄存器
                        instruction.replaceDefVirtualRegister(this.virtualRegisterAllocResult.get(defVirtualRegister), defVirtualRegister);
                    } else {
                        // 在此之前该临时虚拟寄存器未成功分配到临时寄存器，尝试分配
                        PhysicalRegister physicalRegister = this.acquireTempPhysicalRegister(targetBasicBlock.parent());
                        if (physicalRegister == null) {
                            // 已无更多临时寄存器可供分配，分配调度寄存器$v1，写入内存
                            instruction.replaceDefVirtualRegister(PhysicalRegister.V1, defVirtualRegister);
                            Store storeVirtualRegister = new Store(null, Store.SIZE.WORD,
                                    PhysicalRegister.V1, defVirtualRegister.address());
                            storeVirtualRegister.listNode().insertAfter(instructionNode);
                        } else {
                            // 分配到了临时寄存器，进行登记
                            instruction.replaceDefVirtualRegister(physicalRegister, defVirtualRegister);
                            this.virtualRegisterAllocResult.put(defVirtualRegister, physicalRegister);
                        }
                    }
                    // 尝试释放
                    if (this.virtualRegisterAllocResult.containsKey(defVirtualRegister)) {
                        if (this.virtualRegisterAccessPlaces.get(defVirtualRegister).indexOf(instructionNode)
                                == this.virtualRegisterAccessPlaces.get(defVirtualRegister).size() - 1) {
                            this.releaseTempPhysicalRegister(this.virtualRegisterAllocResult.get(defVirtualRegister));
                        }
                    }
                }
            }
        }
        if (this.tempRegisters.size() != 10) {
            throw new IllegalStateException("When allocTempRegister(), the tempRegisters size is " + this.tempRegisters.size() +
                    " after the allocation of TargetBasicBlock " + targetBasicBlock.label());
        }
    }

    private PhysicalRegister acquireTempPhysicalRegister(TargetFunction targetFunction) {
        PhysicalRegister resultRegister = this.tempRegisters.poll();
        targetFunction.stackFrame.ensureSaveRegister(resultRegister);
        return resultRegister;
    }

    private void releaseTempPhysicalRegister(PhysicalRegister physicalRegister) {
        this.tempRegisters.push(physicalRegister);
    }
}
