package output;

import backend.target.TargetModule;
import global.Config;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DumpMIPSAssembly {
    public static void dump(TargetModule targetModule) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(Config.dumpMIPSAssemblyFileName));

        out.close();
    }

    private DumpMIPSAssembly() {
    }
}
