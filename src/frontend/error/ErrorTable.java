package frontend.error;

import java.util.TreeSet;

public class ErrorTable {
    private final TreeSet<ErrorRecord> errorRecords = new TreeSet<>();

    public void addErrorRecord(int line, ErrorType type) {
        this.errorRecords.add(new ErrorRecord(line, type, ""));
    }

    public void addErrorRecord(int line, ErrorType type, String info) {
        this.errorRecords.add(new ErrorRecord(line, type, info));
    }

    public boolean notEmpty() {
        return !this.errorRecords.isEmpty();
    }

    public TreeSet<ErrorRecord> getTreeSetCopy() {
        return new TreeSet<>(this.errorRecords);
    }
}
