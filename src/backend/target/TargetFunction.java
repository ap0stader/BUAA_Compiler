package backend.target;

import IR.value.IRBasicBlock;

import java.util.LinkedList;

public class TargetFunction {
    private final String name;
    // TODO 根据实际需要修改使用的类，必要时自己构建
    private final LinkedList<TargetBasicBlock> basicBlocks;

    public TargetFunction(String name) {
        this.name = name;
        basicBlocks = new LinkedList<>();
    }

    public String mipsStr() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ===== Start of Function ").append(name).append("() >>>>> #\n");
        sb.append(name).append(":\n");
        this.basicBlocks.forEach((B) -> sb.append(B.mipsStr()).append("\n"));
        sb.append("# <<<<<   End of Function ").append(name).append("() ===== #\n");
        sb.append("""
                li $a0, 255 # protected area, should not reach here.
                li $v0, 17  # protected area, should not reach here.
                syscall     # protected area, should not reach here.
                """);
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.mipsStr();
    }
}
