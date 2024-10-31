package IR.value;

import IR.IRValue;
import IR.type.FunctionType;
import IR.type.IRType;
import util.LLVMStrRegCounter;

import java.util.ArrayList;
import java.util.LinkedList;

public class Function extends IRValue<FunctionType> {
    // Function是User的子类，因为Function一旦定义后其地址是一定的
    // 但是在Sysy中，Function不会与任何其他的Value构成足够称为User的关系（Argument和BasicBlock类似属于关系），故直接提升为Value的子类

    private final ArrayList<Argument> arguments;
    // TODO 根据实际需要修改使用的类，必要时自己构建
    private final LinkedList<BasicBlock> basicBlocks;
    private final boolean isLib;

    public Function(String name, FunctionType functionType) {
        super(name, functionType);
        this.arguments = null;
        this.basicBlocks = null;
        this.isLib = true;
    }

    public Function(String name, FunctionType functionType, ArrayList<Argument> arguments) {
        super(name, functionType);
        if (type.parametersType().size() != arguments.size()) {
            throw new RuntimeException("When Function(), number of arguments mismatch. Got " + arguments.size() +
                    ", expected " + type.parametersType().size() + "(declare at the type of function)");
        } else {
            for (int i = 0; i < type.parametersType().size(); i++) {
                if (!IRType.isEqual(type.parametersType().get(i), arguments.get(i).type())) {
                    throw new RuntimeException("When Function(), type mismatch. Got " + arguments.get(i).type() +
                            " of argument " + arguments.get(i) +
                            ", expected " + type.parametersType().get(i) + "(declare at the type of function)");
                }
            }
        }
        this.arguments = arguments;
        this.basicBlocks = new LinkedList<>();
        this.isLib = false;
    }

    public void appendBasicBlock(BasicBlock basicBlock) {
        this.basicBlocks.add(basicBlock);
    }

    public String llvmStr() {
        StringBuilder sb = new StringBuilder();
        LLVMStrRegCounter counter = new LLVMStrRegCounter();
        if (this.isLib) {
            // 函数的声明
            sb.append("declare dso_local ");
        } else {
            // 函数的定义
            sb.append("define dso_local ");
        }
        sb.append(this.type.returnType().llvmStr());
        sb.append(" @");
        sb.append(this.name);
        sb.append("(");
        if (this.isLib) {
            for (int i = 0; i < this.type.parametersType().size(); i++) {
                sb.append(i > 0 ? ", " : "");
                sb.append(this.type.parametersType().get(i).llvmStr());
            }
        } else {
            for (int i = 0; i < this.arguments.size(); i++) {
                sb.append(i > 0 ? ", " : "");
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
                    sb.append("\n");
                    sb.append(counter.get(this.basicBlocks.get(i)).substring(1)).append(":\n");
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
