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
        // 注：
        while (c != EOF) {
            if (c == '\n') {
                this.newLine(); // 记录行号
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
        this.gotToken(TokenType.EOF, "");
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
            case "main" -> this.gotToken(TokenType.MAINTK, indetStr);
            case "const" -> this.gotToken(TokenType.CONSTTK, indetStr);
            case "int" -> this.gotToken(TokenType.INTTK, indetStr);
            case "void" -> this.gotToken(TokenType.VOIDTK, indetStr);
            case "break" -> this.gotToken(TokenType.BREAKTK, indetStr);
            case "continue" -> this.gotToken(TokenType.CONTINUETK, indetStr);
            case "if" -> this.gotToken(TokenType.IFTK, indetStr);
            case "else" -> this.gotToken(TokenType.ELSETK, indetStr);
            case "for" -> this.gotToken(TokenType.FORTK, indetStr);
            case "return" -> this.gotToken(TokenType.RETURNTK, indetStr);
            case "getint" -> this.gotToken(TokenType.GETINTTK, indetStr);
            case "printf" -> this.gotToken(TokenType.PRINTFTK, indetStr);
            default -> this.gotToken(TokenType.IDENFR, indetStr);
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

    private void lexFormatString() throws IOException {
        StringBuilder formatStringStrBuilder = new StringBuilder();
        formatStringStrBuilder.append('"');
        fgetc();
        while (c != '"') {
            // 合法字符：ASCII为32, 33, 37, 40-126的ASCII字符
            // （合法）32:(space) 33:! 37:%
            // （非法）34:" 35:# 36:$ 38:& 39:'
            if (c == 32 || c == 33 || c == 37 || (40 <= c && c <= 126)) {
                formatStringStrBuilder.append(c);
                if (c == 37) {
                    // '%' (37) 出现当且仅当为'%d'
                    fgetc();
                    if (c == 'd') {
                        formatStringStrBuilder.append(c);
                    } else {
                        // 可能其他分支需要使用
                        ungetc();
                        ErrorTable.addErrorRecord(line, ErrorType.ILLEGAL_FORMATSTRING_CHAR, "Got '%', but not %d");
                    }
                } else if (c == 92) {
                    // '\' (92) 出现当且仅当为'\n'
                    fgetc();
                    if (c == 'n') {
                        formatStringStrBuilder.append(c);
                    } else {
                        // 可能其他分支需要使用
                        ungetc();
                        ErrorTable.addErrorRecord(line, ErrorType.ILLEGAL_FORMATSTRING_CHAR, "Got '\\', but not \\n");
                    }
                }
            } else {
                ErrorTable.addErrorRecord(line, ErrorType.ILLEGAL_FORMATSTRING_CHAR, "Got '" + c + "'(ASCII:" + (int) c + ")");
            }
            fgetc();
        } // UNSTABLE 此处没有考虑字符串中非法的换行导致的行数统计错误的问题
        formatStringStrBuilder.append('"');
        fgetc();
        String formatStringStr = formatStringStrBuilder.toString();
        this.gotToken(TokenType.STRCON, formatStringStr);
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
                    if (Config.lexerThrowable) {
                        throw new RuntimeException("When Lexer.lexSymbolComment()->case '&', unexpected character: " + c);
                    } else {
                        ungetc();
                    }
                }
            }
            case '|' -> {
                fgetc();
                if (c == '|') {
                    this.gotToken(TokenType.OR, "||");
                } else {
                    if (Config.lexerThrowable) {
                        throw new RuntimeException("When Lexer.lexSymbolComment()->case '|', unexpected character: " + c);
                    } else {
                        ungetc();
                    }
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
