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
                jal main      # Jump to main()
                
                move $a0, $v0
                li $v0, 17
                syscall       # Terminate
                """);
        out.write("# <<<<<   End of _start procedure ===== #\n\n\n");
        out.write("# ===== Start of Functions >>>>> #\n");
        for (TargetFunction function : targetModule.functions()) {
            out.write(function.mipsStr());
            out.write("\n\n");
        }
        out.write("# <<<<<   End of Functions ===== #");
        out.close();
    }

    private DumpMIPSAssembly() {
    }
}
