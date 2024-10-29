package IR.type;

import java.util.ArrayList;
import java.util.Objects;

public record StructType(ArrayList<IRType> memberTypes)
        implements IRType {
    // 结构体类型，在SysY中没有结构体类型，这个类是为了大型数组的初始化准备的

    @Override
    public String displayStr() {
        throw new UnsupportedOperationException("A StructType should not be display in SysY.");
    }

    @Override
    public String llvmStr() {
        StringBuilder sb = new StringBuilder();
        // 结构体默认为packed的
        sb.append("<{ ");
        for (int i = 0; i < this.memberTypes.size(); i++) {
            sb.append(i > 0 ? ", " : "");
            sb.append(this.memberTypes.get(i).llvmStr());
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
