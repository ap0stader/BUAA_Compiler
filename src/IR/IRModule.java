package IR;

import IR.value.IRFunction;
import IR.value.GlobalVariable;

import java.util.ArrayList;

public class IRModule {
    private final ArrayList<GlobalVariable> globalVariables;
    private final ArrayList<IRFunction> functions;

    public IRModule() {
        this.globalVariables = new ArrayList<>();
        this.functions = new ArrayList<>();
    }

    public ArrayList<GlobalVariable> globalVariables() {
        return globalVariables;
    }

    public ArrayList<IRFunction> functions() {
        return functions;
    }

    public void appendGlobalVariables(GlobalVariable globalVariable) {
        this.globalVariables.add(globalVariable);
    }

    public void appendFunctions(IRFunction function) {
        this.functions.add(function);
    }
}
