package backend.target;

import backend.instruction.TargetInstruction;
import backend.oprand.Label;
import util.DoublyLinkedList;

public class TargetBasicBlock {
    private final Label label;
    private final DoublyLinkedList<TargetInstruction> instructions;
    private final TargetFunction parent;

    public TargetBasicBlock(TargetFunction parent, int order) {
        this.label = new Label(parent.label().name() + "." + order);
        this.instructions = new DoublyLinkedList<>();
        this.parent = parent;
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
}
