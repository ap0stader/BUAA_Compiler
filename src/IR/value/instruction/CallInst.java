package IR.value.instruction;

import IR.IRValue;
import IR.type.VoidType;
import IR.value.BasicBlock;
import IR.value.Function;
import util.LLVMStrRegCounter;

import java.util.ArrayList;

// <result> = call <ty> <fnptrval>(<function args>)
public class CallInst extends Instruction {
    // CallInst不处理对于函数不合法的调用，由语义分析部分予以检查
    public CallInst(Function function, ArrayList<IRValue> argsOperands, BasicBlock parent) {
        super(function.type(), parent);
        this.addOperand(argsOperands.get(0));
        for (IRValue argument : argsOperands) {
            this.addOperand(argument);
        }
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
            sb.append(this.getOperand(i).type().llvmStr());
            sb.append(counter.get(this.getOperand(i)));
        }
        sb.append(")");
        return sb.toString();
    }
}
