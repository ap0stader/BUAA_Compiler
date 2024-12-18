package IR.value.instruction;

import IR.IRUser;
import IR.type.IRType;
import IR.value.IRBasicBlock;
import util.DoublyLinkedList;
import util.LLVMStrRegCounter;

public abstract class IRInstruction<IT extends IRType> extends IRUser<IT> {
    private final DoublyLinkedList.Node<IRInstruction<?>> listNode;

    public IRInstruction(IT type, IRBasicBlock parent) {
        super(type);
        this.listNode = new DoublyLinkedList.Node<>(this);
        // 加入到BasicBlock中
        if (parent != null) {
            parent.appendInstruction(this);
        }
    }

    public DoublyLinkedList.Node<IRInstruction<?>> listNode() {
        return listNode;
    }

    public void eliminate() {
        for (int i = 0; i < this.getNumOperands(); i++) {
            this.getOperand(i).removeUserAllUse(this);
        }
        this.listNode.eliminate();
    }

    public abstract String llvmStr(LLVMStrRegCounter counter);

    @Override
    public String toString() {
        return this.llvmStr(new LLVMStrRegCounter());
    }
}
