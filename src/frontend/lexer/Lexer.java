package frontend.lexer;

import frontend.type.TokenType;

import java.io.IOException;
import java.io.PushbackReader;

public class Lexer {
    private final PushbackReader reader;
    private char c = 0;
    private int line = 1;
    private boolean finish = false;

    private final TokenStream stream = new TokenStream();

    private static final char EOF = (char) -1;

    // Java的Character的实现是基于Unicode的，使用有一定风险，故重写
    private static boolean isDigit(char ch) {
        return '0' <= ch && ch <= '9';
    }

    private static boolean isLetter(char ch) {
        return ('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z');
    }

    private static boolean isLetterOrDigit(char ch) {
        return isLetter(ch) || isDigit(ch);
    }

    public Lexer(PushbackReader reader) {
        this.reader = reader;
    }

    // 从文件中读入下一个字符
    private void fgetc() throws IOException {
        c = (char) this.reader.read();
    }

    // 退回读入的字符
    private void ungetc() throws IOException {
        this.reader.unread(c);
    }

    // 返回从文件中生成的TokenStream
    public TokenStream getTokenStream() throws IOException {
        // 如果已经完成生成TokenStream，直接返回结果
        if (finish) {
            return this.stream;
        }
        fgetc();
        // 约定为每一次循环结束之后都保证c为预读好的一个字符
        // 注：
        while (c != EOF) {
            if (c == '\n') {
                this.line++; // 记录行号
                fgetc();
            } else if (c == ' ' || c == '\t' || c == '\f' || c == '\r') {
                fgetc(); // 跳过空白符号
            } else if (c == '_' || isLetter(c)) {
                this.lexIndetKeyword(); // 标识符或关键字
            } else if (isDigit(c)) {
                this.lexIntConst(); // 整型数字常量
            } else if (c == '"') {
                this.lexFormatString(); // 输出格式字符串
            } else {
                this.lexSymbolComment(); // 各种符号或注释，若为未知字符则直接跳过
            }
        }
        // 加入类型为EOF的Token，表示TokenStream结束
        stream.addToken(new Token(TokenType.EOF, "", this.line));
        this.finish = true;
        return this.stream;
    }

    private void lexIndetKeyword() throws IOException {
        StringBuilder indetStrBuilder = new StringBuilder();
        while (c == '_' || isLetterOrDigit(c)) {
            indetStrBuilder.append(c);
            fgetc();
        }
        String indetStr = indetStrBuilder.toString();
        switch (indetStr) {
            case "main" -> this.stream.addToken(new Token(TokenType.MAINTK, indetStr, this.line));
            case "const" -> this.stream.addToken(new Token(TokenType.CONSTTK, indetStr, this.line));
            case "int" -> this.stream.addToken(new Token(TokenType.INTTK, indetStr, this.line));
            case "void" -> this.stream.addToken(new Token(TokenType.VOIDTK, indetStr, this.line));
            case "break" -> this.stream.addToken(new Token(TokenType.BREAKTK, indetStr, this.line));
            case "continue" -> this.stream.addToken(new Token(TokenType.CONTINUETK, indetStr, this.line));
            case "if" -> this.stream.addToken(new Token(TokenType.IFTK, indetStr, this.line));
            case "else" -> this.stream.addToken(new Token(TokenType.ELSETK, indetStr, this.line));
            case "for" -> this.stream.addToken(new Token(TokenType.FORTK, indetStr, this.line));
            case "return" -> this.stream.addToken(new Token(TokenType.RETURNTK, indetStr, this.line));
            case "getint" -> this.stream.addToken(new Token(TokenType.GETINTTK, indetStr, this.line));
            case "printf" -> this.stream.addToken(new Token(TokenType.PRINTFTK, indetStr, this.line));
            default -> this.stream.addToken(new Token(TokenType.IDENFR, indetStr, this.line));
        }
    }

    private void lexIntConst() throws IOException {
        StringBuilder intConstStrBuilder = new StringBuilder();
        while (isDigit(c)) {
            intConstStrBuilder.append(c);
            fgetc();
        }
        String intConstStr = intConstStrBuilder.toString();
        this.stream.addToken(new Token(TokenType.INTCON, intConstStr, this.line));
    }

    private void lexFormatString() throws IOException {
        StringBuilder formatStringStrBuilder = new StringBuilder();
        formatStringStrBuilder.append('"');
        fgetc();
        while (c != '"') {
            formatStringStrBuilder.append(c);
            fgetc();
        } // UNSTABLE 此处没有考虑字符串的换行问题
        formatStringStrBuilder.append('"');
        fgetc();
        String formatStringStr = formatStringStrBuilder.toString();
        this.stream.addToken(new Token(TokenType.STRCON, formatStringStr, this.line));
    }

    private void lexSymbolComment() throws IOException {
        switch (c) {
            case '/' -> {
                fgetc();
                if (c == '/' || c == '*') {
                    this.lexComment();
                    return; // 由于单行注释有可能直接读到结尾，故直接return以不执行本函数的预读，将预读给到lexComment
                } else {
                    ungetc();
                    this.stream.addToken(new Token(TokenType.DIV, "/", this.line));
                }
            }
            case '+' -> this.stream.addToken(new Token(TokenType.PLUS, "+", this.line));
            case '-' -> this.stream.addToken(new Token(TokenType.MINU, "-", this.line));
            case '*' -> this.stream.addToken(new Token(TokenType.MULT, "*", this.line));
            case '%' -> this.stream.addToken(new Token(TokenType.MOD, "%", this.line));
            case '!' -> {
                fgetc();
                if (c == '=') {
                    this.stream.addToken(new Token(TokenType.NEQ, "!=", this.line));
                } else {
                    ungetc();
                    this.stream.addToken(new Token(TokenType.NOT, "!", this.line));
                }
            }
            case '&' -> {
                fgetc();
                if (c == '&') {
                    this.stream.addToken(new Token(TokenType.AND, "&&", this.line));
                } else {
                    ungetc();
                    // UNSTABLE 错误
                }
            }
            case '|' -> {
                fgetc();
                if (c == '|') {
                    this.stream.addToken(new Token(TokenType.OR, "||", this.line));
                } else {
                    ungetc();
                    // UNSTABLE 错误
                }
            }
            case '<' -> {
                fgetc();
                if (c == '=') {
                    this.stream.addToken(new Token(TokenType.LEQ, "<=", this.line));
                } else {
                    ungetc();
                    this.stream.addToken(new Token(TokenType.LSS, "<", this.line));
                }
            }
            case '>' -> {
                fgetc();
                if (c == '=') {
                    this.stream.addToken(new Token(TokenType.GEQ, ">=", this.line));
                } else {
                    ungetc();
                    this.stream.addToken(new Token(TokenType.GRE, ">", this.line));
                }
            }
            case '=' -> {
                fgetc();
                if (c == '=') {
                    this.stream.addToken(new Token(TokenType.EQL, "==", this.line));
                } else {
                    ungetc();
                    this.stream.addToken(new Token(TokenType.ASSIGN, "=", this.line));
                }
            }
            case ';' -> this.stream.addToken(new Token(TokenType.SEMICN, ";", this.line));
            case ',' -> this.stream.addToken(new Token(TokenType.COMMA, ",", this.line));
            case '(' -> this.stream.addToken(new Token(TokenType.LPARENT, "(", this.line));
            case ')' -> this.stream.addToken(new Token(TokenType.RPARENT, ")", this.line));
            case '[' -> this.stream.addToken(new Token(TokenType.LBRACK, "[", this.line));
            case ']' -> this.stream.addToken(new Token(TokenType.RBRACK, "]", this.line));
            case '{' -> this.stream.addToken(new Token(TokenType.LBRACE, "{", this.line));
            case '}' -> this.stream.addToken(new Token(TokenType.RBRACE, "}", this.line));
            // UNSTABLE default -> 错误
        }
        fgetc();
    }

    private void lexComment() throws IOException {
        if (c == '/') {
            // 单行注释
            while (c != '\n' && c != EOF) {
                fgetc();
            }
            // 如果是换行符说明后续还有内容，需要预读。如果已经是EOF，则保持EOF无需预读
            if (c != EOF) {
                fgetc();
            }
        } else if (c == '*') {
            // 多行注释
            while(c != EOF) {
                // 多行注释可能跨行，注意维护行号
                if (c == '\n') {
                    this.line++;
                } else if (c == '*') {
                    fgetc();
                    if (c == '/') {
                        break;
                    } else {
                        ungetc();
                    }
                }
                fgetc();
            }
            if (c != EOF) {
                fgetc();
            } // else UNSTABLE 错误
        }
    }
}
