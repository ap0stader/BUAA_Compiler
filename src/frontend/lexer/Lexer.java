package frontend.lexer;

import frontend.type.TokenType;

import java.io.IOException;
import java.io.PushbackReader;

public class Lexer {
    private final PushbackReader reader;
    private char c = 0;
    private int line = 1;

    private static final char EOF = (char) -1;

    public Lexer(PushbackReader reader) {
        this.reader = reader;
    }

    // 从文件中读入下一个字符
    private void fgetc() throws IOException {
        c = (char) reader.read();
    }

    // 退回读入的字符
    private void ungetc() throws IOException {
        reader.unread(c);
    }

    // 返回从文件中生成的TokenStream
    public TokenStream generateTokenStream() throws IOException {
        TokenStream tokenStream = new TokenStream();
        fgetc();
        while (c != EOF) {
            switch (c) {
                case '\r':
                    fgetc(); // 如果换行符是CRLF则跳过\r
                case '\n':
                    this.line++; // 记录行号
                    break;
            }
            fgetc();
        }
        tokenStream.addToken(new Token(TokenType.EOF, "", this.line));
        return tokenStream;
    }
}
