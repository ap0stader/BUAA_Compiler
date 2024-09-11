package global.error;

import java.util.ArrayList;

public class ErrorTable {
    private static final ArrayList<ErrorRecord> errorRecords = new ArrayList<>();

    private ErrorTable() {
    }

    public static void addErrorRecord(int line, ErrorType type) {
        errorRecords.add(new ErrorRecord(line, type));
    }

    public static boolean notEmpty() {
        return !errorRecords.isEmpty();
    }

    public static ArrayList<ErrorRecord> getSortedArrayListCopy() {
        ArrayList<ErrorRecord> ret = new ArrayList<>(errorRecords);
        // 每一行中最多只有一个错误，Java的sort()的排序是稳定的，先出现的错误排序后依然在前面
        ret.sort(ErrorRecord::compareTo);
        return ret;
    }
}
