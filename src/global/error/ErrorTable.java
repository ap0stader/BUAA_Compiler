package global.error;

import java.util.TreeSet;

public class ErrorTable {
    private static final TreeSet<ErrorRecord> errorRecords = new TreeSet<>();

    private ErrorTable() {
    }

    public static void addErrorRecord(int line, ErrorType type) {
        errorRecords.add(new ErrorRecord(line, type, ""));
    }

    public static void addErrorRecord(int line, ErrorType type, String info) {
        errorRecords.add(new ErrorRecord(line, type, info));
    }

    public static boolean notEmpty() {
        return !errorRecords.isEmpty();
    }

    public static TreeSet<ErrorRecord> getTreeSetCopy() {
        return new TreeSet<>(errorRecords);
    }
}
