package IR;

import IR.type.IRType;

import java.util.ArrayList;

public abstract class IRUser<T extends IRType> extends IRValue<T> {
    // 在Sysy中，User的子类仅限于Instruction
    private final ArrayList<IRValue<?>> operands;

    public IRUser(T type) {
        super(type);
        this.operands = new ArrayList<>();
    }

    protected void addOperand(IRValue<?> operand) {
        this.operands.add(operand);
        operand.addUse(this);
    }

    public IRValue<?> getOperand(int index) {
        return this.operands.get(index);
    }

    public int getNumOperands() {
        return this.operands.size();
    }
}
