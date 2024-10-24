package IR.type;

import java.util.ArrayList;
import java.util.Objects;

public record StructType(ArrayList<IRType> memberTypes)
        implements IRType {
    // 这个类是为了大型数组的初始化准备的，故默认为packed的
    @Override
    public String displayStr() {
        throw new UnsupportedOperationException("A StructType should not be display in SysY.");
    }

    @Override
    public String llvmStr() {
        StringBuilder sb = new StringBuilder();
        sb.append("<{ ");
        if (!this.memberTypes.isEmpty()) {
            sb.append(this.memberTypes.get(0).llvmStr());
            for (int i = 1; i < this.memberTypes.size(); i++) {
                sb.append(", ");
                sb.append(this.memberTypes.get(i).llvmStr());
            }
        }
        sb.append(" }>");
        return sb.toString();
    }

    // DEBUG 重写toString方法以供调试
    @Override
    public String toString() {
        return llvmStr();
    }

    // WARNING 未重写hashCode方法，不得在Hash类容器中使用
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StructType otherStructType) {
            // ArrayList的比较方法自动严格比较
            return Objects.equals(this.memberTypes, otherStructType.memberTypes);
        } else {
            return false;
        }
    }
}
