package IR;

import IR.value.Function;
import IR.value.GlobalVariable;

import java.util.ArrayList;

public record IRModule(ArrayList<GlobalVariable> globalVariables, ArrayList<Function> functions) {
}
