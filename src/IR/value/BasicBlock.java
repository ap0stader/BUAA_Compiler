package IR.value;

import IR.IRValue;
import IR.type.IRType;

public class BasicBlock extends IRValue {
    public BasicBlock() {
        super(IRType.getLabelTy());
    }
}
