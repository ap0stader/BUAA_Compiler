package output;

import IR.IRModule;
import IR.value.IRFunction;
import IR.value.GlobalVariable;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DumpLLVM {
    public static void dump(IRModule irModule, String filename) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(filename));
        for (GlobalVariable globalVariable : irModule.globalVariables()) {
            out.write(globalVariable.llvmStr());
        }
        out.write("\n");
        for (IRFunction function : irModule.functions()) {
            out.write(function.llvmStr());
            out.write("\n");
        }
        out.close();
    }

    private DumpLLVM() {
    }
}
