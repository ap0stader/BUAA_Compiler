package backend.target;

import backend.oprand.*;

import java.util.LinkedList;
import java.util.TreeSet;

public class TargetFunction {
    private final Label label;
    private final Label labelPrologue;
    private final Label labelEpilogue;
    public final StackFrame stackFrame;
    private final TreeSet<VirtualRegister> virtualRegisters = new TreeSet<>();
    // TODO 根据实际需要修改使用的类，必要时自己构建
    private final LinkedList<TargetBasicBlock> basicBlocks;

    public TargetFunction(String name) {
        this.label = new Label(name);
        this.labelPrologue = new Label(name + ".prologue");
        this.labelEpilogue = new Label(name + ".epilogue");
        this.stackFrame = new StackFrame();
        this.basicBlocks = new LinkedList<>();
    }

    public Label label() {
        return label;
    }

    public Label labelEpilogue() {
        return labelEpilogue;
    }

    public LinkedList<TargetBasicBlock> basicBlocks() {
        return basicBlocks;
    }

    public VirtualRegister addVirtualRegister() {
        VirtualRegister newRegister = VirtualRegister.create();
        this.virtualRegisters.add(newRegister);
        return newRegister;
    }

    public void appendBasicBlock(TargetBasicBlock basicBlock) {
        this.basicBlocks.add(basicBlock);
    }

    public String mipsStr() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ===== Start of Function ").append(label.name()).append("() >>>>> #\n");
        sb.append(this.label.mipsStr()).append(":\n");
        // 函数序言
        sb.append(this.stackFrame.prologue());
        for (TargetBasicBlock targetBasicBlock : basicBlocks) {
            sb.append(targetBasicBlock.mipsStr());
        }
        // 函数尾声
        sb.append(this.stackFrame.epilogue());
        sb.append("# <<<<<   End of Function ").append(label.name()).append("() ===== #\n");
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
        /*
            根据MIPS Calling Convention，从上至下分别为
            调用本函数的参数
            ================ 上级函数的$sp
            临时变量（虚拟寄存器）
            ----------------
            局部变量
            ----------------
            保存的各通用寄存器（寄存器编号越大，地址越高）
            ----------------
            调用其他函数的参数（为$a0-$a3至少预留16字节，参数次序越大，地址越高）
            ================ 本级函数的$sp
        */
        private final TreeSet<PhysicalRegister> savedArgumentRegisters = new TreeSet<>();
        private int allocaSize = 0;
        private final TreeSet<PhysicalRegister> savedRegisters = new TreeSet<>();
        private int argumentsNumbers = 4;

        private final static int REGISTER_BYTES = 4;

        // 栈帧的大小
        private int size() {
            return virtualRegisters.size() * REGISTER_BYTES +
                    this.allocaSize +
                    this.savedRegisters.size() * REGISTER_BYTES +
                    this.argumentsNumbers * REGISTER_BYTES;
        }

        // 传入参数的偏移值
        private final class InArgumentOffset implements TargetAddress.ImmediateOffset {
            private final int number;

            public InArgumentOffset(int number) {
                this.number = number;
            }

            @Override
            public Immediate calc() {
                return new Immediate(size() + this.number * REGISTER_BYTES);
            }

            @Override
            public String toString() {
                return "InArgumentOffset{" +
                        "number=" + number +
                        '}';
            }
        }

        // 临时变量（虚拟寄存器）的偏移值
        private final class VirtualRegisterOffset implements TargetAddress.ImmediateOffset {
            private final VirtualRegister register;

            public VirtualRegisterOffset(VirtualRegister register) {
                this.register = register;
            }

            @Override
            public Immediate calc() {
                if (virtualRegisters.contains(this.register)) {
                    return new Immediate(virtualRegisters.headSet(this.register).size() * REGISTER_BYTES +
                            allocaSize +
                            savedRegisters.size() * REGISTER_BYTES +
                            argumentsNumbers * REGISTER_BYTES);
                } else {
                    throw new RuntimeException("When VirtualRegisterOffset.calc(), the function " + label.name() +
                            "does not use virtual register " + this.register);
                }
            }

            @Override
            public String toString() {
                return "VirtualRegisterOffset{" +
                        "register=" + register +
                        '}';
            }
        }

        // 局部变量的偏移值
        private final class allocaOffset implements TargetAddress.ImmediateOffset {
            private final int accumulateSizeBefore;

            public allocaOffset(int accumulateSizeBefore) {
                this.accumulateSizeBefore = accumulateSizeBefore;
            }

            @Override
            public Immediate calc() {
                return new Immediate(this.accumulateSizeBefore +
                        savedRegisters.size() * REGISTER_BYTES +
                        argumentsNumbers * REGISTER_BYTES);
            }

            @Override
            public String toString() {
                return "allocaOffset{" +
                        "accumulateSizeBefore=" + accumulateSizeBefore +
                        '}';
            }
        }

        // 保存寄存器的偏移值
        private final class SavedRegisterOffset implements TargetAddress.ImmediateOffset {
            private final PhysicalRegister register;

            public SavedRegisterOffset(PhysicalRegister register) {
                this.register = register;
            }

            @Override
            public Immediate calc() {
                if (savedRegisters.contains(this.register)) {
                    return new Immediate(savedRegisters.headSet(this.register).size() * REGISTER_BYTES +
                            argumentsNumbers * REGISTER_BYTES);
                } else {
                    throw new RuntimeException("When SavedRegisterOffset.calc(), function " + label.name() +
                            "dose not save register " + this.register);
                }
            }

            @Override
            public String toString() {
                return "SavedRegisterOffset{" +
                        "register=" + register +
                        '}';
            }
        }

        // 传出参数的偏移值
        private record OutArgumentOffset(int number) implements TargetAddress.ImmediateOffset {
            @Override
            public Immediate calc() {
                return new Immediate(this.number * REGISTER_BYTES);
            }
        }

        // 传入参数的地址，在IRFunction中的第一个IRBasicBlock中，即argBlock中使用
        public RegisterBaseAddress getInArgumentAddress(int argumentNumber) {
            return new RegisterBaseAddress(PhysicalRegister.SP, new InArgumentOffset(argumentNumber));
        }

        // 临时变量（虚拟寄存器）的地址
        public RegisterBaseAddress getVirtualRegisterAddress(VirtualRegister virtualRegister) {
            return new RegisterBaseAddress(PhysicalRegister.SP, new VirtualRegisterOffset(virtualRegister));
        }

        // 局部变量的地址
        public RegisterBaseAddress alloc(int size) {
            int accumulateSizeBefore = this.allocaSize;
            // (getBitWidth + 3) / 4 * 4 即 getBitWidth % 4 == 0 ? getBitWidth : getBitWidth + (4 - getBitWidth % 4);
            this.allocaSize += (size + 3) / REGISTER_BYTES * REGISTER_BYTES;
            return new RegisterBaseAddress(PhysicalRegister.SP, new allocaOffset(accumulateSizeBefore));
        }

        // 保存寄存器的地址
        private RegisterBaseAddress getSavedRegisterAddress(PhysicalRegister savedRegister) {
            if (PhysicalRegister.isArgumentRegister(savedRegister)) {
                return new RegisterBaseAddress(PhysicalRegister.SP,
                        new InArgumentOffset(PhysicalRegister.argumentNumberOfArgumentRegister(savedRegister)));
            } else {
                return new RegisterBaseAddress(PhysicalRegister.SP,
                        new SavedRegisterOffset(savedRegister));
            }
        }

        // 确保保存了$ra寄存器
        public void ensureSaveRA() {
            this.ensureSaveRegister(PhysicalRegister.RA);
        }

        // 确保保存了寄存器
        public void ensureSaveRegister(PhysicalRegister register) {
            if (PhysicalRegister.isArgumentRegister(register)) {
                this.savedArgumentRegisters.add(register);
            } else {
                this.savedRegisters.add(register);
            }
        }

        // 获得传出参数的地址
        public RegisterBaseAddress getOutArgumentAddress(int argumentNumber) {
            this.argumentsNumbers = Math.max(this.argumentsNumbers, argumentNumber);
            return new RegisterBaseAddress(PhysicalRegister.SP, new OutArgumentOffset(argumentNumber));
        }

        // 函数序言
        private String prologue() {
            StringBuilder sb = new StringBuilder();
            sb.append(labelPrologue.mipsStr()).append(":\n");
            // 调整栈的大小
            sb.append("\t").append("# stack frame size").append(this.size()).append(" bytes\n");
            sb.append("\t").append("addiu $sp, $sp, 0x").append(Integer.toHexString(-this.size()).toUpperCase()).append("\n");
            // 保存需要保存的参数寄存器
            for (PhysicalRegister savedArgumentRegister : this.savedArgumentRegisters) {
                sb.append("\t").append("sw ").append(savedArgumentRegister.mipsStr()).append(", ")
                        .append(this.getSavedRegisterAddress(savedArgumentRegister).mipsStr()).append("\n");
            }
            // 保存需要使用的寄存器
            for (PhysicalRegister savedRegister : this.savedRegisters.descendingSet()) {
                sb.append("\t").append("sw ").append(savedRegister.mipsStr()).append(", ")
                        .append(this.getSavedRegisterAddress(savedRegister).mipsStr()).append("\n");
            }
            return sb.toString();
        }

        // 函数尾声
        private String epilogue() {
            StringBuilder sb = new StringBuilder();
            sb.append(labelEpilogue.mipsStr()).append(":\n");
            // 恢复被使用的寄存器
            for (PhysicalRegister savedRegister : this.savedRegisters) {
                sb.append("\t").append("lw ").append(savedRegister.mipsStr()).append(", ")
                        .append(this.getSavedRegisterAddress(savedRegister).mipsStr()).append("\n");
            }
            // 调整栈的大小
            sb.append("\t").append("# stack frame size").append(this.size()).append(" bytes\n");
            sb.append("\t").append("addiu $sp, $sp, 0x").append(Integer.toHexString(this.size()).toUpperCase()).append("\n");
            // 返回到之前的函数
            sb.append("\t").append("jr $ra").append("\n");
            return sb.toString();
        }
    }
}
