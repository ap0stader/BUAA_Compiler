package IR.value.instruction;

import IR.IRUser;
import IR.type.IRType;
import IR.value.IRBasicBlock;
import util.DoublyLinkedList;
import util.LLVMStrRegCounter;

public abstract class IRInstruction<IT extends IRType> extends IRUser<IT> {
    private final DoublyLinkedList.Node<IRInstruction<?>> listNode;
    protected IRBasicBlock parent;

    public IRInstruction(IT type, IRBasicBlock parent) {
        super(type);
        this.listNode = new DoublyLinkedList.Node<>(this);
        this.parent = parent;
        // 自动加入到BasicBlock中
        if (parent != null) {
            parent.appendInstruction(this);
        }
    }

    public DoublyLinkedList.Node<IRInstruction<?>> listNode() {
        return listNode;
    }

    public IRBasicBlock parent() {
        return parent;
    }

    public void setParent(IRBasicBlock parent) {
        this.parent = parent;
    }

    // WARNING 调用本方法后该指令必须被移除
    public void dropAllOperands() {
        for (int i = 0; i < this.getNumOperands(); i++) {
            this.getOperand(i).removeUser(this);
        }
    }

    // WARNING 不得边迭代边调用本方法
    public void eliminate() {
        if (this.users.isEmpty()) {
            this.dropAllOperands();
            this.listNode.eliminate();
        } else {
            throw new RuntimeException("When eliminate(), try to eliminate an instruction that was used. ");
        }
    }

    // WARNING 仅适用于大规模确保正确的删除
    public void eliminateWithoutCheck() {
        this.dropAllOperands();
        this.listNode.eliminate();
    }

    public abstract String llvmStr(LLVMStrRegCounter counter);

    @Override
    public String toString() {
        return this.llvmStr(new LLVMStrRegCounter());
    }
}
