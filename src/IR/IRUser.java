package IR;

import IR.type.IRType;

import java.util.ArrayList;
import java.util.Objects;

public abstract class IRUser<T extends IRType> extends IRValue<T> {
    // 在Sysy中，User的子类仅限于Instruction
    private final ArrayList<IRValue<?>> operands;

    public IRUser(T type) {
        super(type);
        this.operands = new ArrayList<>();
    }

    protected void addOperand(IRValue<?> operand) {
        this.operands.add(operand);
        operand.addUser(this);
    }

    public int getNumOperands() {
        return this.operands.size();
    }

    public IRValue<?> getOperand(int index) {
        return this.operands.get(index);
    }

    // 将所有对于某个operand的使用进行替换
    public void replaceUsesOfWith(IRValue<?> operand, IRValue<?> replacement) {
        for (int i = 0; i < operands.size(); i++) {
            if (Objects.equals(operands.get(i), operand)) {
                this.operands.set(i, replacement);
                replacement.addUser(this);
                operand.removeUser(this);
            }
        }
    }
}
