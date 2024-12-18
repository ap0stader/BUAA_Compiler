package IR.value;

import IR.IRValue;
import IR.type.IRType;
import IR.type.LabelType;
import IR.value.instruction.IRInstruction;
import util.DoublyLinkedList;
import util.LLVMStrRegCounter;

public class IRBasicBlock extends IRValue<LabelType> {
    private final DoublyLinkedList<IRInstruction<?>> instructions;

    public IRBasicBlock() {
        super(IRType.getLabelTy());
        this.instructions = new DoublyLinkedList<>();
    }

    public DoublyLinkedList<IRInstruction<?>> instructions() {
        return instructions;
    }

    public void appendInstruction(IRInstruction<?> instructionNode) {
        this.instructions.insertAfterTail(instructionNode.listNode());
    }

    public String llvmStr(LLVMStrRegCounter counter) {
        StringBuilder sb = new StringBuilder();
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
