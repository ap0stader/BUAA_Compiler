package IR.value;

import IR.IRValue;
import IR.type.IRType;
import IR.type.PointerType;
import IR.value.constant.IRConstant;

public class IRGlobalVariable extends IRValue<PointerType> {
    // GlobalVariable是User的子类，因为GlobalVariable一旦定义后其地址是一定的
    // 但是其使用的Constant已不维护Use，故直接提升为Value的子类

    private final boolean isConstant;
    private final boolean isPrivate;
    private final IRConstant<?> initVals;

    public IRGlobalVariable(String name, IRType type,
                            boolean setConstant, boolean setPrivate,
                            IRConstant<?> initVals) {
        // 自动转为对应的指针类型，不需要在传入时包装为指针类型
        super(name, new PointerType(type));
        this.isConstant = setConstant;
        this.isPrivate = setPrivate;
        // 进行传入类型与初始值的匹配检查
        if (!IRType.isEqual(type, initVals.type())) {
            throw new RuntimeException("When GlobalVariable(), " + name + " type mismatch. Got " + type +
                    ", but type of initVals is " + initVals.type());
        }
        this.initVals = initVals;

    }

    public IRConstant<?> initVals() {
        return initVals;
    }

    public String llvmStr() {
        return "@" + this.name + " = " +
                (this.isPrivate ? "private unnamed_addr " : "")+
                (this.isConstant ? "constant" : "global") + " " +
                this.type.referenceType().llvmStr() + " " +
                this.initVals.llvmStr() + "\n";
    }

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        return this.llvmStr();
    }
}
