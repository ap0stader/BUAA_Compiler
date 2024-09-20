package frontend.error;

public record ErrorRecord(int line, ErrorType type, String info) implements Comparable<ErrorRecord> {
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ErrorRecord record) {
            // 规定每一行中最多只有一个错误
            // 一行发生了多个错误时，只记录第一个错误
            return this.line == record.line;
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(ErrorRecord o) {
        return Integer.compare(this.line, o.line);
    }
}
