package output;

import IR.IRModule;
import IR.value.GlobalVariable;
import global.Config;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DumpLLVM {
    public static void dump(IRModule irModule) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(Config.dumpLLVMBeforeOptimizedFileName));
        for (GlobalVariable globalVariable : irModule.globalVariables()) {
            out.write(globalVariable.llvmStr());
        }
        out.close();
    }

    private DumpLLVM() {
    }
}
