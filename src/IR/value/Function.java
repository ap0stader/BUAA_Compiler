package IR.value;

import IR.IRValue;
import IR.type.FunctionType;
import util.LLVMStrRegCounter;

import java.util.ArrayList;
import java.util.LinkedList;

public class Function extends IRValue {
    // Function是User的子类，因为Function一旦定义后其地址是一定的
    // 但是在Sysy中，Function不会与任何其他的Value构成足够称为User的关系（Argument和BasicBlock类似属于关系），故直接提升为Value的子类
    // 在Sysy中，对于Function的使用仅限于CallInst，并且做未被使用函数删除并没有显著效果，故不维护包括其的Use

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
        if (this.isLib) {
            sb.append("\n");
        } else {
            if (this.basicBlocks.isEmpty()) {
                throw new RuntimeException("When Function.llvmStr(), the Function of " + this.name +
                        " has no basic blocks, but it is not a library function");
            } else {
                sb.append(" {\n");
                // 第一个基本块占用命名资源，但是不输出
                counter.get(this.basicBlocks.get(0));
                sb.append(this.basicBlocks.get(0).llvmStr(counter));
                for (int i = 1; i < this.basicBlocks.size(); i++) {
                    sb.append(this.basicBlocks.get(i)).append(":\n");
                    sb.append(this.basicBlocks.get(i).llvmStr(counter));
                }
                sb.append("}\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.llvmStr();
    }
}
