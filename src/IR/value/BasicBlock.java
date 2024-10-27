package IR.value;

import IR.IRValue;
import IR.type.IRType;
import IR.value.instruction.Instruction;
import util.LLVMStrRegCounter;

import java.util.LinkedList;

public class BasicBlock extends IRValue {
    // TODO 根据实际需要修改使用的类，必要时自己构建
    private final LinkedList<Instruction> instructions;
    private final Function parent;

    public BasicBlock(Function parent) {
        super(IRType.getLabelTy());
        this.instructions = new LinkedList<>();
        this.parent = parent;
    }

    public void appendInstruction(Instruction instruction) {
        this.instructions.add(instruction);
    }

    public Function parent() {
        return parent;
    }

    public String llvmStr(LLVMStrRegCounter counter) {
        StringBuilder sb = new StringBuilder();
        for (Instruction instruction : instructions) {
            sb.append("    ");
            sb.append(instruction.llvmStr(counter));
            sb.append("\n");
        }
        return sb.toString();
    }

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        return this.llvmStr(new LLVMStrRegCounter());
    }
}
