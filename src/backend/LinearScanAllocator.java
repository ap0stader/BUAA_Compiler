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
import util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class LinearScanAllocator {
    private final TargetModule targetModule;
    private boolean finish = false;

    private TargetFunction nowFunction;

    // 临时寄存器
    private final LinkedList<PhysicalRegister> tempRegisters;
    // 保存变量寄存器
    private final LinkedList<PhysicalRegister> savedRegisters;

    // 保存一个函数中基本块的DFS顺序
    private final ArrayList<TargetBasicBlock> basicBlockDFSOrder;
    // 保存一个函数各基本块的Use的虚拟寄存器
    private final HashMap<TargetBasicBlock, HashSet<VirtualRegister>> basicBlockUses;
    // 保存一个函数各基本块的Def的虚拟寄存器
    private final HashMap<TargetBasicBlock, HashSet<VirtualRegister>> basicBlockDefs;
    // 保存一个函数各基本块以基本块为单位进行活跃变量分析的活跃的虚拟寄存器
    private final HashMap<TargetBasicBlock, HashSet<VirtualRegister>> basicBlockLiveRegister;
    // 保存一个函数中跨越了基本块的虚拟寄存器
    private final HashSet<VirtualRegister> virtualRegisterCrossBasicBlock;

    // 保存一个函数中指令的DFS顺序
    private final ArrayList<TargetInstruction> instructionDFSOrder;
    // 保存一个函数各指令以指令为单位进行活跃变量分析的活跃的虚拟寄存器
    private final HashMap<TargetInstruction, HashSet<VirtualRegister>> instructionLiveRegister;
    // 保存一个函数各非预定义的跨越基本块的虚拟寄存器的开始活跃指令顺序号
    private final HashMap<VirtualRegister, Integer> liveRegisterStartInstructionIndex;
    // 保存一个函数各非预定义的跨越基本块的虚拟寄存器的结束活跃指令顺序号
    private final HashMap<VirtualRegister, Integer> liveRegisterEndInstructionIndex;

    // 保存一个函数中一条指令Use的虚拟寄存器使用的调度寄存器
    private final HashMap<TargetInstruction, HashSet<PhysicalRegister>> instructionUseUsedDispatchRegisters;

    // 保存一个函数中的虚拟寄存器的分配及过
    private final HashMap<VirtualRegister, PhysicalRegister> allocationResults;

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

        this.basicBlockDFSOrder = new ArrayList<>();
        this.basicBlockUses = new HashMap<>();
        this.basicBlockDefs = new HashMap<>();
        this.basicBlockLiveRegister = new HashMap<>();
        this.virtualRegisterCrossBasicBlock = new HashSet<>();

        this.instructionDFSOrder = new ArrayList<>();
        this.instructionLiveRegister = new HashMap<>();
        this.liveRegisterStartInstructionIndex = new HashMap<>();
        this.liveRegisterEndInstructionIndex = new HashMap<>();

        this.instructionUseUsedDispatchRegisters = new HashMap<>();

        this.allocationResults = new HashMap<>();
    }

    private void initSavedRegisters() {
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

    private PhysicalRegister acquireUseDispatchRegister(TargetInstruction targetInstruction) {
        HashSet<PhysicalRegister> used = this.instructionUseUsedDispatchRegisters.get(targetInstruction);
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
        this.initSavedRegisters();
        this.nowFunction = targetFunction;

        this.basicBlockDFSOrder.clear();
        this.basicBlockUses.clear();
        this.basicBlockDefs.clear();
        this.basicBlockLiveRegister.clear();
        this.virtualRegisterCrossBasicBlock.clear();

        this.instructionDFSOrder.clear();
        this.instructionLiveRegister.clear();
        this.liveRegisterStartInstructionIndex.clear();
        this.liveRegisterEndInstructionIndex.clear();

        this.instructionUseUsedDispatchRegisters.clear();

        this.allocationResults.clear();
        // 第一步：对基本块进行DFS生成基础信息，包括指令排序，基本块的Def和Use
        this.generateBasicInfo(targetFunction.basicBlocks().head().value());
        // 第二步：以基本块为单位进行活跃变量分析
        this.generateLiveInfo(targetFunction);
        // 第三步：分配临时寄存器
        targetFunction.basicBlocks().forEach(basicBlockNode -> this.allocTempRegister(basicBlockNode.value()));
        // 第四步：分配全局寄存器
        this.allocGlobalRegister();
        // 第五步：处理预定义寄存器
        targetFunction.basicBlocks().forEach(basicBlockNode -> this.replaceRegister(basicBlockNode.value()));
    }

    private void generateBasicInfo(TargetBasicBlock targetBasicBlock) {
        this.basicBlockDFSOrder.add(targetBasicBlock);
        HashSet<VirtualRegister> useVirtualRegisters = new HashSet<>();
        HashSet<VirtualRegister> defVirtualRegisters = new HashSet<>();
        for (DoublyLinkedList.Node<TargetInstruction> instructionNode : targetBasicBlock.instructions()) {
            TargetInstruction instruction = instructionNode.value();
            this.instructionDFSOrder.add(instruction);
            // 添加到基本块的Use和Def集合中，注意预定义虚拟寄存器不参与分析
            // 对于不跨越基本块的临时虚拟寄存器，都是先定义后进行使用的
            // 各类指令都是先Use寄存器，然后再Def寄存器的
            for (VirtualRegister useVirtualRegister : instruction.useVirtualRegisterSet()) {
                if (!useVirtualRegister.isPreDefined() && !defVirtualRegisters.contains(useVirtualRegister)) {
                    useVirtualRegisters.add(useVirtualRegister);
                }
            }
            for (VirtualRegister defVirtualRegister : instruction.defVirtualRegisterSet()) {
                if (!defVirtualRegister.isPreDefined() && !useVirtualRegisters.contains(defVirtualRegister)) {
                    defVirtualRegisters.add(defVirtualRegister);
                }
            }
            // 初始化该指令Use的虚拟寄存器使用的调度寄存器
            this.instructionUseUsedDispatchRegisters.put(instruction, new HashSet<>());
        }
        this.basicBlockUses.put(targetBasicBlock, useVirtualRegisters);
        this.basicBlockDefs.put(targetBasicBlock, defVirtualRegisters);
        this.basicBlockLiveRegister.put(targetBasicBlock, new HashSet<>());
        if (targetBasicBlock.trueSuccessor() != null &&
                !this.basicBlockDFSOrder.contains(targetBasicBlock.trueSuccessor())) {
            this.generateBasicInfo(targetBasicBlock.trueSuccessor());
        }
        if (targetBasicBlock.falseSuccessor() != null &&
                !this.basicBlockDFSOrder.contains(targetBasicBlock.falseSuccessor())) {
            this.generateBasicInfo(targetBasicBlock.falseSuccessor());
        }
    }

    private void generateLiveInfo(TargetFunction targetFunction) {
        boolean hasUpdates;
        do {
            hasUpdates = false;
            // 从后向前计算迭代次数最少
            DoublyLinkedList.Node<TargetBasicBlock> nowTargetBasicBlockNode = targetFunction.basicBlocks().tail();
            while (nowTargetBasicBlockNode != null) {
                TargetBasicBlock targetBasicBlock = nowTargetBasicBlockNode.value();
                // 计算out
                HashSet<VirtualRegister> outVirtualRegisters = new HashSet<>();
                if (targetBasicBlock.trueSuccessor() != null) {
                    outVirtualRegisters.addAll(this.basicBlockLiveRegister.get(targetBasicBlock.trueSuccessor()));
                }
                if (targetBasicBlock.falseSuccessor() != null) {
                    outVirtualRegisters.addAll(this.basicBlockLiveRegister.get(targetBasicBlock.falseSuccessor()));
                }
                // 计算in
                // in[B] = use[B] + (out[B] - def[B])
                HashSet<VirtualRegister> inVirtualRegisters = new HashSet<>(this.basicBlockUses.get(targetBasicBlock));
                outVirtualRegisters.removeAll(this.basicBlockDefs.get(targetBasicBlock));
                inVirtualRegisters.addAll(outVirtualRegisters);
                // 检查是否有更新
                if (!inVirtualRegisters.equals(this.basicBlockLiveRegister.get(targetBasicBlock))) {
                    this.basicBlockLiveRegister.put(targetBasicBlock, inVirtualRegisters);
                    this.virtualRegisterCrossBasicBlock.addAll(inVirtualRegisters);
                    hasUpdates = true;
                }
                // 向前移动
                nowTargetBasicBlockNode = nowTargetBasicBlockNode.pred();
            }
        } while (hasUpdates);
        // 检查第一个基本块是否无活跃变量信息以初步检验计算结果是否正确
        if (!this.basicBlockLiveRegister.get(this.basicBlockDFSOrder.get(0)).isEmpty()) {
            throw new IllegalStateException("When generateLiveInfo(), the first basic block should not have live register info");
        }
        for (DoublyLinkedList.Node<TargetBasicBlock> targetBasicBlockNode : targetFunction.basicBlocks()) {
            TargetBasicBlock targetBasicBlock = targetBasicBlockNode.value();
            // 最后一个语句的out[S] = out[B]
            HashSet<VirtualRegister> outVirtualRegisters = new HashSet<>();
            if (targetBasicBlock.trueSuccessor() != null) {
                outVirtualRegisters.addAll(this.basicBlockLiveRegister.get(targetBasicBlock.trueSuccessor()));
            }
            if (targetBasicBlock.falseSuccessor() != null) {
                outVirtualRegisters.addAll(this.basicBlockLiveRegister.get(targetBasicBlock.falseSuccessor()));
            }
            DoublyLinkedList.Node<TargetInstruction> nowInstructionNode = targetBasicBlock.instructions().tail();
            while (nowInstructionNode != null) {
                TargetInstruction instruction = nowInstructionNode.value();
                // 计算in
                // in[S] = use[S] + (out[S] - def[S])
                outVirtualRegisters.removeAll(instruction.defVirtualRegisterSet().stream()
                        .filter((this.virtualRegisterCrossBasicBlock::contains)).collect(Collectors.toSet()));
                HashSet<VirtualRegister> inVirtualRegisters = instruction.useVirtualRegisterSet().stream()
                        .filter(this.virtualRegisterCrossBasicBlock::contains).collect(Collectors.toCollection(HashSet::new));
                inVirtualRegisters.addAll(outVirtualRegisters);
                this.instructionLiveRegister.put(instruction, inVirtualRegisters);
                outVirtualRegisters = new HashSet<>(inVirtualRegisters);
                // 向前移动
                nowInstructionNode = nowInstructionNode.pred();
            }
        }
        // 检查第一个指令是否无活跃变量信息以初步检验计算结果是否正确
        if (!this.instructionLiveRegister.get(this.instructionDFSOrder.get(0)).isEmpty()) {
            throw new IllegalStateException("When generateLiveInfo(), the first instruction should not have live register info");
        }
        // 计算每个跨越基本块活跃的虚拟寄存器的活跃区间
        for (int i = 0; i < this.instructionDFSOrder.size(); i++) {
            TargetInstruction instruction = this.instructionDFSOrder.get(i);
            for (VirtualRegister liveVirtualRegister : this.instructionLiveRegister.get(instruction)) {
                this.liveRegisterStartInstructionIndex.putIfAbsent(liveVirtualRegister, i);
                this.liveRegisterEndInstructionIndex.put(liveVirtualRegister, i);
            }
        }
    }

    private void allocTempRegister(TargetBasicBlock targetBasicBlock) {
        HashMap<VirtualRegister, LinkedList<TargetInstruction>> accessPlaces = new HashMap<>();
        // 计算该基本块每个虚拟寄存器的访问位置
        for (DoublyLinkedList.Node<TargetInstruction> instructionNode : targetBasicBlock.instructions()) {
            TargetInstruction instruction = instructionNode.value();
            instructionNode.value().useVirtualRegisterSet()
                    .forEach(virtualRegister -> {
                        accessPlaces.putIfAbsent(virtualRegister, new LinkedList<>());
                        accessPlaces.get(virtualRegister).add(instruction);
                    });
            instructionNode.value().defVirtualRegisterSet()
                    .forEach(virtualRegister -> {
                        accessPlaces.putIfAbsent(virtualRegister, new LinkedList<>());
                        accessPlaces.get(virtualRegister).add(instruction);
                    });
        }
        // 分配临时寄存器
        for (DoublyLinkedList.Node<TargetInstruction> instructionNode : targetBasicBlock.instructions()) {
            TargetInstruction instruction = instructionNode.value();
            for (VirtualRegister useVirtualRegister : instruction.useVirtualRegisterSet()) {
                if (!this.virtualRegisterCrossBasicBlock.contains(useVirtualRegister) && !useVirtualRegister.isPreDefined()) {
                    // 完成一次访问
                    if (!instruction.equals(accessPlaces.get(useVirtualRegister).pop())) {
                        throw new IllegalStateException("When allocTempRegister(), the order of accessPlaces is unexpected");
                    }
                    // 尝试释放
                    if (this.allocationResults.containsKey(useVirtualRegister) &&
                            accessPlaces.get(useVirtualRegister).isEmpty()) {
                        this.releaseTempPhysicalRegister(this.allocationResults.get(useVirtualRegister));
                    }
                }
            }
            for (VirtualRegister defVirtualRegister : instruction.defVirtualRegisterSet()) {
                if (!this.virtualRegisterCrossBasicBlock.contains(defVirtualRegister) && !defVirtualRegister.isPreDefined()) {
                    if (!this.allocationResults.containsKey(defVirtualRegister)) {
                        // 在此之前该临时虚拟寄存器未成功分配到临时寄存器，尝试分配
                        PhysicalRegister physicalRegister = this.acquireTempPhysicalRegister();
                        if (physicalRegister != null) {
                            // 分配到了临时寄存器，进行登记
                            this.allocationResults.put(defVirtualRegister, physicalRegister);
                        }
                    }
                    // 完成一次访问
                    if (!instruction.equals(accessPlaces.get(defVirtualRegister).pop())) {
                        throw new IllegalStateException("When allocTempRegister(), the order of accessPlaces is unexpected");
                    }
                    // 尝试释放
                    if (this.allocationResults.containsKey(defVirtualRegister) &&
                            accessPlaces.get(defVirtualRegister).isEmpty()) {
                        this.releaseTempPhysicalRegister(this.allocationResults.get(defVirtualRegister));
                    }
                }
            }
        }
        if (this.tempRegisters.size() != PhysicalRegister.TEMP_REGISTER_SIZE) {
            throw new IllegalStateException("When allocTempRegister(), the size of tempRegisters is " + this.tempRegisters.size() +
                    " after the allocation of TargetBasicBlock " + targetBasicBlock.label());
        }
    }

    private PhysicalRegister acquireTempPhysicalRegister() {
        PhysicalRegister resultRegister = this.tempRegisters.poll();
        this.nowFunction.stackFrame.ensurePrologueSaveRegister(resultRegister);
        return resultRegister;
    }

    private void releaseTempPhysicalRegister(PhysicalRegister physicalRegister) {
        this.tempRegisters.push(physicalRegister);
    }

    private void allocGlobalRegister() {
        for (int i = 0; i < this.instructionDFSOrder.size(); i++) {
            // 分配寄存器
            // 筛选出开始活跃的寄存器
            ArrayList<Pair<VirtualRegister, Integer>> nowStartVirtualRegisterEndPairs = new ArrayList<>();
            for (Map.Entry<VirtualRegister, Integer> startEntry : this.liveRegisterStartInstructionIndex.entrySet()) {
                VirtualRegister virtualRegister = startEntry.getKey();
                Integer startIndex = startEntry.getValue();
                if (startIndex == i) {
                    nowStartVirtualRegisterEndPairs.add(new Pair<>(virtualRegister, this.liveRegisterEndInstructionIndex.get(virtualRegister)));
                }
            }
            nowStartVirtualRegisterEndPairs.sort(Comparator.comparingInt(Pair::value));
            for (Pair<VirtualRegister, Integer> nowStartVirtualRegisterEndInfo : nowStartVirtualRegisterEndPairs) {
                VirtualRegister startVirtualRegister = nowStartVirtualRegisterEndInfo.key();
                // 尝试分配全局寄存器
                PhysicalRegister physicalRegister = this.acquireSavedPhysicalRegister();
                if (physicalRegister != null) {
                    // 分配到了全局寄存器，进行登记
                    this.allocationResults.put(startVirtualRegister, physicalRegister);
                }
            }
            // 筛选出之后不再活跃的寄存器
            for (Map.Entry<VirtualRegister, Integer> endEntry : this.liveRegisterEndInstructionIndex.entrySet()) {
                VirtualRegister endVirtualRegister = endEntry.getKey();
                Integer endIndex = endEntry.getValue();
                if (endIndex == i) {
                    // 尝试释放
                    if (this.allocationResults.containsKey(endVirtualRegister)) {
                        this.releaseSavedPhysicalRegister(this.allocationResults.get(endVirtualRegister));
                    }
                }
            }
        }
        if (this.savedRegisters.size() != PhysicalRegister.SAVED_REGISTER_SIZE) {
            throw new IllegalStateException("When allocGlobalRegister(), the size of savedRegisters is " + this.savedRegisters.size() +
                    " after the allocation of TargetFunction " + nowFunction.label());
        }
    }

    private PhysicalRegister acquireSavedPhysicalRegister() {
        PhysicalRegister resultRegister = this.savedRegisters.poll();
        this.nowFunction.stackFrame.ensurePrologueSaveRegister(resultRegister);
        return resultRegister;
    }

    private void releaseSavedPhysicalRegister(PhysicalRegister physicalRegister) {
        this.savedRegisters.push(physicalRegister);
    }

    private void replaceRegister(TargetBasicBlock targetBasicBlock) {
        for (DoublyLinkedList.Node<TargetInstruction> instructionNode : targetBasicBlock.instructions()) {
            TargetInstruction instruction = instructionNode.value();
            // 对于不跨越基本块的临时虚拟寄存器，都是先定义后进行使用的
            for (VirtualRegister useVirtualRegister : instruction.useVirtualRegisterSet()) {
                if (!useVirtualRegister.isPreAllocated()) {
                    if (this.allocationResults.containsKey(useVirtualRegister)) {
                        // 分配到了寄存器
                        instruction.replaceUseVirtualRegister(this.allocationResults.get(useVirtualRegister), useVirtualRegister);
                    } else {
                        // 未成功分配到临时寄存器，分配调度寄存器，从内存当中加载
                        PhysicalRegister physicalRegister = this.acquireUseDispatchRegister(instruction);
                        Load loadVirtualRegister = new Load(null, Load.SIZE.WORD,
                                physicalRegister, useVirtualRegister.address());
                        loadVirtualRegister.listNode().insertBefore(instructionNode);
                        instruction.replaceUseVirtualRegister(physicalRegister, useVirtualRegister);
                    }
                }
            }
            for (VirtualRegister defVirtualRegister : instruction.defVirtualRegisterSet()) {
                if (!defVirtualRegister.isPreAllocated()) {
                    // 分配到了寄存器
                    if (this.allocationResults.containsKey(defVirtualRegister)) {
                        instruction.replaceDefVirtualRegister(this.allocationResults.get(defVirtualRegister), defVirtualRegister);
                    } else {
                        // 未成功分配到临时寄存器，分配调度寄存器，保存到内存当中
                        instruction.replaceDefVirtualRegister(PhysicalRegister.V1, defVirtualRegister);
                        Store storeVirtualRegister = new Store(null, Store.SIZE.WORD,
                                PhysicalRegister.V1, defVirtualRegister.address());
                        storeVirtualRegister.listNode().insertAfter(instructionNode);
                    }
                }
            }
        }
    }
}
