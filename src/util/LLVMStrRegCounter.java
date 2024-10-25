package util;

import IR.IRValue;

import java.util.HashMap;

public class LLVMStrRegCounter {
    private final HashMap<IRValue, Integer> llvmRegTable;
    private int count;

    public LLVMStrRegCounter() {
        this.llvmRegTable = new HashMap<>();
        this.count = 0;
    }

    public String get(IRValue value) {
        if (this.llvmRegTable.containsKey(value)) {
            return "%" + this.llvmRegTable.get(value);
        } else {
            this.llvmRegTable.put(value, this.count);
            return "%" + this.count++;
        }
    }
}
