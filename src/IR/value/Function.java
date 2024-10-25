package IR.value;

import IR.IRValue;
import IR.type.FunctionType;
import util.LLVMStrRegCounter;

import java.util.ArrayList;
import java.util.LinkedList;

public class Function extends IRValue {
    private final ArrayList<Argument> arguments;
    // TODO 根据实际需要修改使用的类，必要时自己构建
    private final LinkedList<BasicBlock> basicBlocks;
    private final boolean isLib;

    public Function(String name, FunctionType type, ArrayList<Argument> arguments, boolean isLib) {
        super(name, type);
        this.arguments = arguments;
        this.basicBlocks = new LinkedList<>();
        this.isLib = isLib;
    }

    public void appendBasicBlock(BasicBlock basicBlock) {
        this.basicBlocks.add(basicBlock);
    }

    public String llvmStr() {
        FunctionType functionType = (FunctionType) this.type;
        StringBuilder sb = new StringBuilder();
        LLVMStrRegCounter counter = new LLVMStrRegCounter();
        if (this.isLib) {
            // 函数的声明
            sb.append("declare dso_local ");
        } else {
            // 函数的定义
            sb.append("define dso_local ");
        }
        sb.append(functionType.returnType().llvmStr());
        sb.append(" @");
        sb.append(this.name);
        sb.append("(");
        if (this.isLib) {
            for (int i = 0; i < functionType.parametersType().size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(functionType.parametersType().get(i).llvmStr());
            }
        } else {
            for (int i = 0; i < this.arguments.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(this.arguments.get(i).llvmStr(counter));
            }
        }
        sb.append(")");
        if (!this.isLib) {
            if (this.basicBlocks.isEmpty()) {
                throw new RuntimeException("When Function.llvmStr(), the Function of " + this.name +
                        " has no basic blocks, but it is not a library function");
            } else {
                sb.append(" {\n");
                counter.get(this.basicBlocks.get(0));
                for (int i = 1; i < this.basicBlocks.size(); i++) {
                    sb.append(this.basicBlocks.get(i));
                    sb.append(":\n");
                    sb.append(this.basicBlocks.get(i).llvmStr(counter));
                }
                sb.append("}\n");
            }
        }
        return sb.toString();
    }
}
