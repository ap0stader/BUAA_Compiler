package IR;

import IR.value.Function;
import IR.value.GlobalVariable;

import java.util.ArrayList;

public class IRModule {
    private final ArrayList<GlobalVariable> globalVariables;
    private final ArrayList<Function> functions;

    public IRModule() {
        this.globalVariables = new ArrayList<>();
        this.functions = new ArrayList<>();
    }

    public ArrayList<GlobalVariable> globalVariables() {
        return globalVariables;
    }

    public ArrayList<Function> functions() {
        return functions;
    }

    public void appendGlobalVariables(GlobalVariable globalVariable) {
        this.globalVariables.add(globalVariable);
    }
}
