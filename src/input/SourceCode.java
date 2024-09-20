package input;

import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;

public class SourceCode {
    private final PushbackReader sourcecodeReader;

    public SourceCode(String inputFilename) throws IOException {
        this.sourcecodeReader = new PushbackReader(new FileReader(inputFilename));
    }

    public PushbackReader reader() {
        return this.sourcecodeReader;
    }

    public void close() throws IOException {
        this.sourcecodeReader.close();
    }
}
