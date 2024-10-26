package IR;

import IR.type.IRType;

import java.util.ArrayList;

public abstract class IRUser extends IRValue {
    // 在Sysy中，User的子类仅限于Instruction
    private final ArrayList<IRValue> operands;

    public IRUser(IRType type) {
        super(type);
        this.operands = new ArrayList<>();
    }

    // TODO 界定可见性范围
    protected void addOperand(IRValue operand) {
        this.operands.add(operand);
        operand.addUse(this);
    }

    public IRValue getOperand(int index) {
        return this.operands.get(index);
    }
}
