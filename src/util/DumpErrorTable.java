package util;

import global.Config;
import global.error.ErrorRecord;
import global.error.ErrorTable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DumpErrorTable {
    public static void dump() throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(Config.dumpErrorTableFileName));
        for (ErrorRecord record : ErrorTable.getSortedArrayListCopy()) {
            out.write(record.line() + " " + record.type() + "\n");
        }
        out.close();
    }

    private DumpErrorTable() {
    }
}
