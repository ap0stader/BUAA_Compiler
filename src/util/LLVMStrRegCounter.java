package util;

import IR.IRValue;
import IR.value.IRFunction;
import IR.value.IRGlobalVariable;
import IR.value.constant.IRConstant;

import java.util.HashMap;

public class LLVMStrRegCounter {
    private final HashMap<IRValue<?>, Integer> llvmRegTable;
    private int count;

    public LLVMStrRegCounter() {
        this.llvmRegTable = new HashMap<>();
        this.count = 0;
    }

    public String get(IRValue<?> value) {
        if (value instanceof IRGlobalVariable || value instanceof IRFunction) {
            return "@" + value.name();
        } else if (value instanceof IRConstant<?> constant) {
            return constant.llvmStr();
        } else {
            // 由于实际上在后续才输出的内容先申请到了寄存器，故临时加入T跳过LLVM的检测
            if (this.llvmRegTable.containsKey(value)) {
                return "%T" + this.llvmRegTable.get(value);
            } else {
                // 为Value分配新的编号
                this.llvmRegTable.put(value, this.count);
                return "%T" + this.count++;
            }
        }
    }
}
