package global.error;

public record ErrorRecord(int line, ErrorType type) implements Comparable<ErrorRecord> {
    @Override
    public int compareTo(ErrorRecord o) {
        // 每一行中最多只有一个错误
        return Integer.compare(this.line, o.line);
    }
}
