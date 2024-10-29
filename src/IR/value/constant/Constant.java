package IR.value.constant;

import IR.IRUser;
import IR.IRValue;
import IR.type.IRType;

public abstract class Constant<CT extends IRType> extends IRValue<CT> {
    // Constant是User的子类，但是在SysY能生成的LLVM IR中，Constant类只有即用即抛的作用
    // 故Constant直接提升为Value的子类，同时不维护包括其的Use

    // 传递构造函数
    protected Constant(CT type) {
        super(type);
    }

    // 对于Constant，即用即抛，不维护包括其的Use
    @Override
    public void addUse(IRUser<?> user) {
    }

    public abstract String llvmStr();

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        return this.llvmStr();
    }
}
