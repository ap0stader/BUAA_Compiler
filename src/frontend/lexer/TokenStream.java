package frontend.lexer;

import config.Config;
import frontend.type.TokenType;

import java.util.ArrayList;
import java.util.Arrays;

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

    // 获取后offset指向的Token
    public Token getNext(int offset) {
        if (this.hasNext() && offset >= 0) {
            return this.list.get(this.pos + offset);
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
    public Token consumeOrNull(TokenType... types) {
        Token nowToken = this.getNow();
        for (TokenType type : types) {
            if (nowToken.type() == type) {
                this.pos++;
                return nowToken;
            }
        }
        return null;
    }

    // 如果当前指向的Token为types中指定的类型，则返回并且指针向后移动一位
    // 否则根据情况决定是否抛出错误
    public Token consumeOrThrow(String place, TokenType... types) {
        if (types.length == 0 && Config.parserThrowable) {
            throw new RuntimeException("When " + place + ", none of type allowed");
        }
        Token ret = this.consumeOrNull(types);
        if (ret == null) {
            if (Config.parserThrowable) {
                throw new RuntimeException("When " + place + ", unexpected token: " + getNow()
                        + ". Expected: " + Arrays.toString(types));
            } else {
                return null;
            }
        } else {
            return ret;
        }
    }

    // 获取stream的ArrayList副本
    public ArrayList<Token> getArrayListCopy() {
        return new ArrayList<>(this.list);
    }
}
