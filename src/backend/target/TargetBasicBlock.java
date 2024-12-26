package backend.target;

import backend.instruction.TargetInstruction;
import backend.oprand.Label;
import util.DoublyLinkedList;

import java.util.HashSet;

public class TargetBasicBlock {
    private final DoublyLinkedList.Node<TargetBasicBlock> listNode;

    private final Label label;
    private final DoublyLinkedList<TargetInstruction> instructions;
    private final TargetFunction parent;
    private final Integer order;

    // 基本块的前驱基本块
    private final HashSet<TargetBasicBlock> predecessors;
    // 由BasicBlock只有结尾一条跳转语句和Branch的生成原则可知一个TargetBasicBlock只有最多两个后继基本块
    // 对于结尾是无条件跳转到其他基本块的，使用trueSuccessor，falseSuccessor保持为null
    // 对于结尾是返回语句跳转到函数尾声的，两个均保持为null
    private TargetBasicBlock trueSuccessor = null;
    private TargetBasicBlock falseSuccessor = null;

    public TargetBasicBlock(TargetFunction parent, int order) {
        this.listNode = new DoublyLinkedList.Node<>(this);
        this.label = new Label(parent.label().name() + "." + order);
        this.instructions = new DoublyLinkedList<>();
        this.parent = parent;
        this.order = order;
        this.predecessors = new HashSet<>();
    }

    public TargetBasicBlock(TargetFunction parent, int fromOrder, int toOrder) {
        this.listNode = new DoublyLinkedList.Node<>(this);
        this.label = new Label(parent.label().name() + "." + fromOrder + ".PHI." + toOrder);
        this.parent = parent;
        this.instructions = new DoublyLinkedList<>();
        this.order = null;
        this.predecessors = new HashSet<>();
    }

    public DoublyLinkedList.Node<TargetBasicBlock> listNode() {
        return listNode;
    }

    public Label label() {
        return label;
    }

    public DoublyLinkedList<TargetInstruction> instructions() {
        return instructions;
    }

    public TargetFunction parent() {
        return parent;
    }

    public Integer order() {
        return order;
    }

    public HashSet<TargetBasicBlock> predecessors() {
        return predecessors;
    }

    public TargetBasicBlock trueSuccessor() {
        return trueSuccessor;
    }

    public TargetBasicBlock falseSuccessor() {
        return falseSuccessor;
    }

    public void appendInstruction(TargetInstruction instruction) {
        this.instructions.insertAfterTail(instruction.listNode());
    }

    public void setTrueSuccessor(TargetBasicBlock trueSuccessor) {
        this.trueSuccessor = trueSuccessor;
    }

    public void setFalseSuccessor(TargetBasicBlock falseSuccessor) {
        this.falseSuccessor = falseSuccessor;
    }

    public String mipsStr() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.label.mipsStr()).append(":\n");
        for (DoublyLinkedList.Node<TargetInstruction> instructionNode : this.instructions) {
            sb.append("\t").append(instructionNode.value().mipsStr()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.mipsStr();
    }
}
