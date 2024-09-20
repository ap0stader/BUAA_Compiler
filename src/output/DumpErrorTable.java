package output;

import global.Config;
import frontend.error.ErrorRecord;
import frontend.error.ErrorTable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DumpErrorTable {
    public static void dump(ErrorTable errorTable) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(Config.dumpErrorTableFileName));
        for (ErrorRecord record : errorTable.getTreeSetCopy()) {
            if (Config.dumpErrorTableDetail) {
                out.write(record.line() + " " + record.type().name() + " " + record.info() + "\n");
            } else {
                out.write(record.line() + " " + record.type() + "\n");
            }
        }
        out.close();
    }

    private DumpErrorTable() {
    }
}
