package IR.value.instruction;

import IR.User;
import util.LLVMStrRegCounter;

public abstract class Instruction extends User {
    // TODO：未完成开发时使用，后续应当改为抽象方法。
    public String llvmStr(LLVMStrRegCounter counter) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
