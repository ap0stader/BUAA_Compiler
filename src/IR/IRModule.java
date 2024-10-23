package IR;

import IR.value.Function;
import IR.value.GlobalVariable;

import java.util.ArrayList;

public class IRModule {
    public final ArrayList<GlobalVariable> globalVariables;
    public final ArrayList<Function> functions;

    public IRModule() {
        this.globalVariables = new ArrayList<>();
        this.functions = new ArrayList<>();
    }
}
