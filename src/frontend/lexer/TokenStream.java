package frontend.lexer;

import frontend.type.TokenType;

import java.util.ArrayList;

public class TokenStream {
    private final ArrayList<Token> list = new ArrayList<>();
    private int pos = 0;

    // 添加Token到TokenStream中，仅同package的Lexer可访问
    void addToken(Token token) {
        this.list.add(token);
    }

    // 重新从头开始读取
    public void restart() {
        this.pos = 0;
    }

    public boolean hasNext() {
        return this.list.get(this.pos).type() != TokenType.EOF;
    }

    // 获取当前指向的Token
    public Token getNow() {
        return this.list.get(this.pos);
    }

    // 获取下一个指向的Token
    public Token getNext() {
        if (this.hasNext()) {
            return this.list.get(this.pos + 1);
        } else {
            return null;
        }
    }

    // 获取当前指向的Token，并指针向后移动一位
    public Token consume() {
        return this.list.get(this.pos++);
    }

    // 如果当前指向的Token为types中指定的类型，则返回并且指针向后移动一位
    // 否则返回null
    public Token consume(TokenType... types) {
        Token nowToken = this.list.get(this.pos);
        for (TokenType type : types) {
            if (nowToken.type() == type) {
                this.pos++;
                return nowToken;
            }
        }
        return null;
    }

    // 获取stream的ArrayList副本
    public ArrayList<Token> getArrayListCopy() {
        return new ArrayList<>(this.list);
    }
}
