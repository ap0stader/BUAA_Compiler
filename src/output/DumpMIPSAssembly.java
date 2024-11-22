package output;

import backend.target.TargetDataObject;
import backend.target.TargetFunction;
import backend.target.TargetModule;
import global.Config;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DumpMIPSAssembly {
    public static void dump(TargetModule targetModule) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(Config.dumpMIPSAssemblyFileName));
        out.write("# ===== Start of DataObjects >>>>> #\n");
        out.write(".data\n");
        for (TargetDataObject dataObject : targetModule.dataObjects()) {
            out.write(dataObject.mipsStr());
        }
        out.write("# <<<<<   End of DataObjects ===== #\n\n\n");
        out.write(".text\n");
        out.write("# ===== Start of _start procedure >>>>> #\n");
        out.write("""
                la $at, main
                jr $at       # Jump to main()
                
                li $v0, 10
                syscall      # Terminate
                """);
        out.write("# <<<<<   End of _start procedure ===== #\n\n\n");
        out.write("# ===== Start of Functions >>>>> #\n");
        for (TargetFunction function : targetModule.functions()) {
            out.write(function.mipsStr());
            out.write("\n");
        }
        out.write("# <<<<<   End of Functions ===== #");
        out.close();
    }

    private DumpMIPSAssembly() {
    }
}
