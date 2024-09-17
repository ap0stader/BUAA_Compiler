package frontend.lexer;

import global.Config;
import frontend.type.TokenType;
import global.error.ErrorTable;
import global.error.ErrorType;

import java.io.IOException;
import java.io.PushbackReader;

public class Lexer {
    private final PushbackReader reader;
    private char c = 0;
    private int line = 1;
    private int indexOfLine = 1;
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

    // 读取到新的一行
    private void newLine() {
        this.line++;
        this.indexOfLine = 1;
    }

    // 解析到一个新的Token，加入到TokenStream中
    private void gotToken(TokenType type, String strVal) {
        this.stream.addToken(new Token(type, strVal, this.line, this.indexOfLine));
        this.indexOfLine++;
    }

    // 返回从文件中生成的TokenStream
    public TokenStream getTokenStream() throws IOException {
        // 如果已经完成生成TokenStream，直接返回结果
        if (finish) {
            return this.stream;
        }
        fgetc();
        // 约定为每一次循环结束之后都保证c为预读好的一个字符
        while (c != EOF) {
            if (c == '\n') {
                this.newLine(); // 记录行号
                fgetc();
            } else if (c == ' ' || c == '\t' || c == '\f' || c == '\r') {
                fgetc(); // 跳过空白符号
            } else if (c == '_' || isLetter(c)) {
                this.lexIdentKeyword(); // 标识符或关键字
            } else if (isDigit(c)) {
                this.lexIntConst(); // 整型数字常量
            } else if (c == '"') {
                this.lexStringConst(); // 字符串常量
            } else if (c == '\'') {
                this.lexCharConst(); // 字符常量
            } else {
                this.lexSymbolComment(); // 各种符号或注释，若为未知字符则直接跳过
            }
        }
        // 加入类型为EOF的Token，表示TokenStream结束
        this.gotToken(TokenType.EOF, "");
        this.finish = true;
        return this.stream;
    }

    private void lexIdentKeyword() throws IOException {
        StringBuilder identStrBuilder = new StringBuilder();
        while (c == '_' || isLetterOrDigit(c)) {
            identStrBuilder.append(c);
            fgetc();
        }
        String identStr = identStrBuilder.toString();
        switch (identStr) {
            case "main" -> this.gotToken(TokenType.MAINTK, identStr);
            case "const" -> this.gotToken(TokenType.CONSTTK, identStr);
            case "int" -> this.gotToken(TokenType.INTTK, identStr);
            case "char" -> this.gotToken(TokenType.CHARTK, identStr);
            case "void" -> this.gotToken(TokenType.VOIDTK, identStr);
            case "break" -> this.gotToken(TokenType.BREAKTK, identStr);
            case "continue" -> this.gotToken(TokenType.CONTINUETK, identStr);
            case "if" -> this.gotToken(TokenType.IFTK, identStr);
            case "else" -> this.gotToken(TokenType.ELSETK, identStr);
            case "for" -> this.gotToken(TokenType.FORTK, identStr);
            case "return" -> this.gotToken(TokenType.RETURNTK, identStr);
            case "getint" -> this.gotToken(TokenType.GETINTTK, identStr);
            case "getchar" -> this.gotToken(TokenType.GETCHARTK, identStr);
            case "printf" -> this.gotToken(TokenType.PRINTFTK, identStr);
            default -> this.gotToken(TokenType.IDENFR, identStr);
        }
    }

    private void lexIntConst() throws IOException {
        StringBuilder intConstStrBuilder = new StringBuilder();
        while (isDigit(c)) {
            intConstStrBuilder.append(c);
            fgetc();
        }
        String intConstStr = intConstStrBuilder.toString();
        this.gotToken(TokenType.INTCON, intConstStr);
    }

    private void lexStringConst() throws IOException {
        StringBuilder formatStringStrBuilder = new StringBuilder();
        formatStringStrBuilder.append('"');
        fgetc();
        while (c != '"') {
            // 包括32-126的所有ASCII字符
            if (32 <= c && c <= 126) {
                formatStringStrBuilder.append(c);
                // '\' (92) 出现需要特别处理转义字符
                if (c == '\\') {
                    fgetc();
                    switch (c) {
                        // 合法的转义字符
                        case 'a', 'b', 't', 'n', 'v', 'f', '"', '\'', '\\', '0':
                            formatStringStrBuilder.append(c);
                            break;
                        default:
                            ungetc();
                            throw new RuntimeException("When lexStringConst(), after receive a \\," +
                                    " got '" + c + "'(ASCII:" + (int) c + ") at line" + this.line + "," +
                                    " expected 'a', 'b', 't', 'n', 'v', 'f', '\\\"', '\\'', '\\\\', '0'");
                    }
                }
            } else {
                throw new RuntimeException("When lexStringConst(), got '" + c + "'(ASCII:" + (int) c + ") at line" + this.line);
            }
            fgetc();
        } // UNSTABLE 此处没有考虑字符串中非法的换行导致的行数统计错误的问题
        formatStringStrBuilder.append('"');
        fgetc();
        String formatStringStr = formatStringStrBuilder.toString();
        this.gotToken(TokenType.STRCON, formatStringStr);
    }

    private void lexCharConst() throws IOException {
        StringBuilder formatStringStrBuilder = new StringBuilder();
        formatStringStrBuilder.append('\'');
        fgetc();
        // 包括32-126的所有ASCII字符
        if (32 <= c && c <= 126) {
            formatStringStrBuilder.append(c);
            // '\' (92) 出现需要特别处理转义字符
            if (c == '\\') {
                fgetc();
                switch (c) {
                    // 合法的转义字符
                    case 'a', 'b', 't', 'n', 'v', 'f', '"', '\'', '\\', '0':
                        formatStringStrBuilder.append(c);
                        break;
                    default:
                        ungetc();
                        throw new RuntimeException("When lexCharConst(), after receive a \\," +
                                " got '" + c + "'(ASCII:" + (int) c + ") at line" + this.line + "," +
                                " expected 'a', 'b', 't', 'n', 'v', 'f', '\\\"', '\\'', '\\\\', '0'");
                }
            }
        } else {
            throw new RuntimeException("When lexCharConst(), got '" + c + "'(ASCII:" + (int) c + ") at line" + this.line);
        }
        fgetc();
        if (c == '\'') {
            formatStringStrBuilder.append('\'');
        } else {
            ungetc();
            throw new RuntimeException("When lexCharConst(), more than one character in single quotation mark at line" + this.line);
        }
        // UNSTABLE 此处没有考虑字符中非法的换行导致的行数统计错误的问题
        fgetc();
        String formatStringStr = formatStringStrBuilder.toString();
        this.gotToken(TokenType.CHRCON, formatStringStr);
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
                    this.gotToken(TokenType.DIV, "/");
                }
            }
            case '+' -> this.gotToken(TokenType.PLUS, "+");
            case '-' -> this.gotToken(TokenType.MINU, "-");
            case '*' -> this.gotToken(TokenType.MULT, "*");
            case '%' -> this.gotToken(TokenType.MOD, "%");
            case '!' -> {
                fgetc();
                if (c == '=') {
                    this.gotToken(TokenType.NEQ, "!=");
                } else {
                    ungetc();
                    this.gotToken(TokenType.NOT, "!");
                }
            }
            case '&' -> {
                fgetc();
                if (c == '&') {
                    this.gotToken(TokenType.AND, "&&");
                } else {
                    this.gotToken(TokenType.AND, "&");
                    ErrorTable.addErrorRecord(this.line, ErrorType.ILLEGAL_AND_OR,
                            "Got '" + c + "'(ASCII:" + (int) c + ") when expected '&'");
                    ungetc();
                }
            }
            case '|' -> {
                fgetc();
                if (c == '|') {
                    this.gotToken(TokenType.OR, "||");
                } else {
                    this.gotToken(TokenType.OR, "|");
                    ErrorTable.addErrorRecord(this.line, ErrorType.ILLEGAL_AND_OR,
                            "Got '" + c + "'(ASCII:" + (int) c + ") when expected '|'");
                    ungetc();
                }
            }
            case '<' -> {
                fgetc();
                if (c == '=') {
                    this.gotToken(TokenType.LEQ, "<=");
                } else {
                    ungetc();
                    this.gotToken(TokenType.LSS, "<");
                }
            }
            case '>' -> {
                fgetc();
                if (c == '=') {
                    this.gotToken(TokenType.GEQ, ">=");
                } else {
                    ungetc();
                    this.gotToken(TokenType.GRE, ">");
                }
            }
            case '=' -> {
                fgetc();
                if (c == '=') {
                    this.gotToken(TokenType.EQL, "==");
                } else {
                    ungetc();
                    this.gotToken(TokenType.ASSIGN, "=");
                }
            }
            case ';' -> this.gotToken(TokenType.SEMICN, ";");
            case ',' -> this.gotToken(TokenType.COMMA, ",");
            case '(' -> this.gotToken(TokenType.LPARENT, "(");
            case ')' -> this.gotToken(TokenType.RPARENT, ")");
            case '[' -> this.gotToken(TokenType.LBRACK, "[");
            case ']' -> this.gotToken(TokenType.RBRACK, "]");
            case '{' -> this.gotToken(TokenType.LBRACE, "{");
            case '}' -> this.gotToken(TokenType.RBRACE, "}");
            default -> {
                if (Config.lexerThrowable) {
                    throw new RuntimeException("When Lexer.lexSymbolComment()->default, unexpected character: " + c);
                }
            }
        }
        fgetc();
    }

    private void lexComment() throws IOException {
        if (c == '/') {
            // 单行注释
            while (c != '\n' && c != EOF) {
                fgetc();
            }
        } else if (c == '*') {
            // 多行注释
            while (c != EOF) {
                // 多行注释可能跨行，注意维护行号
                if (c == '\n') {
                    this.newLine();
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
            } else {
                if (Config.lexerThrowable) {
                    throw new RuntimeException("When Lexer.lexComment(), unexpected EOF");
                }
            }
        }
    }
}
