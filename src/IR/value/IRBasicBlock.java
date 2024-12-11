package IR.value;

import IR.IRValue;
import IR.type.IRType;
import IR.type.LabelType;
import IR.value.instruction.IRInstruction;
import util.LLVMStrRegCounter;

import java.util.LinkedList;

public class IRBasicBlock extends IRValue<LabelType> {
    // TODO 根据实际需要修改使用的类，必要时自己构建
    private final LinkedList<IRInstruction<?>> instructions;
    private final IRFunction parent;

    public IRBasicBlock(IRFunction parent) {
        super(IRType.getLabelTy());
        this.instructions = new LinkedList<>();
        this.parent = parent;
    }

    public LinkedList<IRInstruction<?>> instructions() {
        return instructions;
    }

    public IRFunction parent() {
        return parent;
    }

    public void appendInstruction(IRInstruction<?> instruction) {
        this.instructions.add(instruction);
    }

    public String llvmStr(LLVMStrRegCounter counter) {
        StringBuilder sb = new StringBuilder();
        for (IRInstruction<?> instruction : instructions) {
            sb.append("\t").append(instruction.llvmStr(counter)).append("\n");
        }
        return sb.toString();
    }

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        return this.llvmStr(new LLVMStrRegCounter());
    }
}
