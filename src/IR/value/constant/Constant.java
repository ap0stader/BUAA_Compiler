package IR.value.constant;

import IR.IRValue;
import IR.type.IRType;

public abstract class Constant extends IRValue {
    // 传递构造函数
    protected Constant(IRType type) {
        super(type);
    }
}
