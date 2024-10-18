package IR;

import IR.value.Function;
import IR.value.GlobalVariable;
import IR.value.constant.Constant;

import java.util.ArrayList;

public class IRModule {
    public final ArrayList<GlobalVariable> globalVariables;
    public final ArrayList<Constant> constantData;
    public final ArrayList<Function> functions;

    public IRModule() {
        this.globalVariables = new ArrayList<>();
        this.constantData = new ArrayList<>();
        this.functions = new ArrayList<>();
    }
}
