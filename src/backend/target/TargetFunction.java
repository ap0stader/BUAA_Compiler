package backend.target;

import backend.oprand.Label;

import java.util.LinkedList;

public class TargetFunction {
    private final String name;
    private final Label label;
    private final Label labelPrologue;
    private final Label labelEpilogue;
    public final StackFrame stackFrame;
    // TODO 根据实际需要修改使用的类，必要时自己构建
    private final LinkedList<TargetBasicBlock> basicBlocks;

    public TargetFunction(String name) {
        this.name = name;
        this.label = new Label(name);
        this.labelPrologue = new Label(name + ".prologue");
        this.labelEpilogue = new Label(name + ".epilogue");
        this.stackFrame = new StackFrame();
        this.basicBlocks = new LinkedList<>();
    }

    public String name() {
        return name;
    }

    public void appendBasicBlock(TargetBasicBlock basicBlock) {
        this.basicBlocks.add(basicBlock);
    }

    public String mipsStr() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ===== Start of Function ").append(name).append("() >>>>> #\n");
        sb.append(this.label.mipsStr()).append(":\n");
        // 函数序言
        sb.append(this.stackFrame.prologueStr());
        for (int counter = 0; counter < this.basicBlocks.size(); counter++) {
            sb.append(this.basicBlocks.get(counter).mipsStr(counter));
        }
        // 函数尾声
        sb.append(this.stackFrame.epilogueStr());
        sb.append("# <<<<<   End of Function ").append(name).append("() ===== #\n");
        // 保护区域
        sb.append("""
                li $a0, 255 # protected area, should not reach here.
                li $v0, 17  # protected area, should not reach here.
                syscall     # protected area, should not reach here.""");
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.mipsStr();
    }

    public class StackFrame {
        private String prologueStr() {
            return labelPrologue.mipsStr() + ":\n";

        }

        private String epilogueStr() {
            return labelEpilogue.mipsStr() + ":\n";

        }
    }
}
