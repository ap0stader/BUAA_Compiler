package IR.type;

import java.util.ArrayList;

public final class StructType implements IRType {
    private final ArrayList<IRType> memberType;

    public StructType(ArrayList<IRType> memberTypes) {
        this.memberType = memberTypes;
    }

    @Override
    public String displayStr() {
        throw new UnsupportedOperationException("A StructType should not be display in SysY.");
    }
}
