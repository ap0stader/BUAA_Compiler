package IR.value.instruction;

import IR.IRValue;
import IR.type.IRType;
import IR.type.VoidType;
import IR.value.IRBasicBlock;
import IR.value.IRFunction;
import util.LLVMStrRegCounter;

import java.util.ArrayList;

// <result> = call <ty> <fnptrval>(<function args>)
public class CallInst extends IRInstruction<IRType> {
    // CallInst不处理对于函数不合法的调用，由语义分析部分予以检查

    public CallInst(IRFunction function, ArrayList<IRValue<?>> argsOperands, IRBasicBlock parent) {
        super(function.type().returnType(), parent);
        this.addOperand(function);
        for (IRValue<?> argument : argsOperands) {
            this.addOperand(argument);
        }
    }

    public IRFunction getCalledFunction() {
        // CAST 构造函数限制
        return (IRFunction) this.getOperand(0);
    }

    public int getNumArgs() {
        return this.getNumOperands() - 1;
    }

    public IRValue<?> getArgOperand(int argNo) {
        // CAST 构造函数限制
        return this.getOperand(argNo + 1);
    }

    @Override
    public String llvmStr(LLVMStrRegCounter counter) {
        StringBuilder sb = new StringBuilder();
        if (!(this.type instanceof VoidType)) {
            sb.append(counter.get(this)).append(" = ");
        }
        sb.append("call ");
        sb.append(this.type.llvmStr()).append(" ");
        sb.append(counter.get(this.getOperand(0)));
        sb.append("(");
        for (int i = 1; i < this.getNumOperands(); i++) {
            sb.append(i > 1 ? ", " : "");
            sb.append(this.getOperand(i).type().llvmStr()).append(" ");
            sb.append(counter.get(this.getOperand(i)));
        }
        sb.append(")");
        return sb.toString();
    }
}
