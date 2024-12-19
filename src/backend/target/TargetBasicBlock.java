package backend.target;

import IR.value.IRBasicBlock;
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

    // 前驱基本块
    private final HashSet<TargetBasicBlock> predecessors;
    // 后继基本块
    private final HashSet<TargetBasicBlock> successors;

    public TargetBasicBlock(TargetFunction parent, int order) {
        this.listNode = new DoublyLinkedList.Node<>(this);
        this.label = new Label(parent.label().name() + "." + order);
        this.instructions = new DoublyLinkedList<>();
        this.parent = parent;
        this.order = order;
        this.predecessors = new HashSet<>();
        this.successors = new HashSet<>();
    }

    public TargetBasicBlock(TargetFunction parent, int fromOrder, int toOrder) {
        this.listNode = new DoublyLinkedList.Node<>(this);
        this.label = new Label(parent.label().name() + "." + fromOrder + ".PHI." + toOrder);
        this.parent = parent;
        this.instructions = new DoublyLinkedList<>();
        this.order = null;
        this.predecessors = new HashSet<>();
        this.successors = new HashSet<>();
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

    public HashSet<TargetBasicBlock> successors() {
        return successors;
    }

    public void appendInstruction(TargetInstruction instruction) {
        this.instructions.insertAfterTail(instruction.listNode());
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
