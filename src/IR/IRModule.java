package IR;

import IR.value.IRFunction;
import IR.value.IRGlobalVariable;

import java.util.ArrayList;

public class IRModule {
    private final ArrayList<IRGlobalVariable> globalVariables;
    private final ArrayList<IRFunction> functions;

    public IRModule() {
        this.globalVariables = new ArrayList<>();
        this.functions = new ArrayList<>();
    }

    public ArrayList<IRGlobalVariable> globalVariables() {
        return globalVariables;
    }

    public ArrayList<IRFunction> functions() {
        return functions;
    }

    public void appendGlobalVariables(IRGlobalVariable globalVariable) {
        this.globalVariables.add(globalVariable);
    }

    public void appendFunctions(IRFunction function) {
        this.functions.add(function);
    }
}
