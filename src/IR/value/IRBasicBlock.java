package IR.value;

import IR.IRValue;
import IR.type.IRType;
import IR.type.LabelType;
import IR.value.instruction.IRInstruction;
import global.Config;
import util.DoublyLinkedList;
import util.LLVMStrRegCounter;

import java.util.HashSet;

public class IRBasicBlock extends IRValue<LabelType> {
    private final DoublyLinkedList<IRInstruction<?>> instructions;

    // 前驱基本块
    private final HashSet<IRBasicBlock> predecessors;
    // 后继基本块
    private final HashSet<IRBasicBlock> successors;
    // 严格支配者
    private final HashSet<IRBasicBlock> dominators;
    // 严格支配的基本块
    private final HashSet<IRBasicBlock> dominating;
    // 直接支配者
    private IRBasicBlock immediateDominator;
    // 直接支配的基本块
    private final HashSet<IRBasicBlock> immediateDominating;
    // 支配边界
    private final HashSet<IRBasicBlock> dominanceFrontiers;

    public IRBasicBlock() {
        super(IRType.getLabelTy());
        this.instructions = new DoublyLinkedList<>();
        this.predecessors = new HashSet<>();
        this.successors = new HashSet<>();
        this.dominators = new HashSet<>();
        this.dominating = new HashSet<>();
        this.immediateDominator = null;
        this.immediateDominating = new HashSet<>();
        this.dominanceFrontiers = new HashSet<>();
    }

    public DoublyLinkedList<IRInstruction<?>> instructions() {
        return instructions;
    }

    public HashSet<IRBasicBlock> predecessors() {
        return predecessors;
    }

    public HashSet<IRBasicBlock> successors() {
        return successors;
    }

    public HashSet<IRBasicBlock> dominators() {
        return dominators;
    }

    public HashSet<IRBasicBlock> dominating() {
        return dominating;
    }

    public IRBasicBlock immediateDominator() {
        return immediateDominator;
    }

    public HashSet<IRBasicBlock> immediateDominating() {
        return immediateDominating;
    }

    public HashSet<IRBasicBlock> dominanceFrontiers() {
        return dominanceFrontiers;
    }

    public void pushInstruction(IRInstruction<?> instruction) {
        this.instructions.insertBeforeHead(instruction.listNode());
    }

    public void appendInstruction(IRInstruction<?> instruction) {
        this.instructions.insertAfterTail(instruction.listNode());
    }

    public void setImmediateDominator(IRBasicBlock immediateDominator) {
        this.immediateDominator = immediateDominator;
    }

    public String llvmStr(LLVMStrRegCounter counter) {
        StringBuilder sb = new StringBuilder();
        if (Config.dumpLLVMDetail) {
            sb.append("\t;preds: ");
            for (IRBasicBlock predecessor : predecessors) {
                sb.append(counter.get(predecessor)).append(" ");
            }
            sb.append("\n");
            sb.append("\t;succs: ");
            for (IRBasicBlock successor : successors) {
                sb.append(counter.get(successor)).append(" ");
            }
            sb.append("\n");
            sb.append("\t;dominators: ");
            for (IRBasicBlock dominator : dominators) {
                sb.append(counter.get(dominator)).append(" ");
            }
            sb.append("\n");
            sb.append("\t;dominating: ");
            for (IRBasicBlock dominating : dominating) {
                sb.append(counter.get(dominating)).append(" ");
            }
            sb.append("\n");
            sb.append("\t;immediateDominator: ").append(counter.get(immediateDominator)).append("\n");
            sb.append("\t;immediateDominating: ");
            for (IRBasicBlock immediateDominating : immediateDominating) {
                sb.append(counter.get(immediateDominating)).append(" ");
            }
            sb.append("\n");
            sb.append("\t;dominanceFrontiers: ");
            for (IRBasicBlock dominanceFrontier : dominanceFrontiers) {
                sb.append(counter.get(dominanceFrontier)).append(" ");
            }
            sb.append("\n");
        }
        for (DoublyLinkedList.Node<IRInstruction<?>> instructionNode : this.instructions) {
            sb.append("\t").append(instructionNode.value().llvmStr(counter)).append("\n");
        }
        return sb.toString();
    }

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        return this.llvmStr(new LLVMStrRegCounter());
    }
}
