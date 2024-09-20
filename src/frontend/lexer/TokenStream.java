package frontend.lexer;

import global.Config;
import frontend.type.TokenType;
import frontend.error.ErrorTable;
import frontend.error.ErrorType;

import java.util.ArrayList;
import java.util.Arrays;

public class TokenStream {
    private final ArrayList<Token> list = new ArrayList<>();
    private int pos = 0;

    private final ArrayList<CheckPoint> checkpoints = new ArrayList<>();

    private final ErrorTable errorTable;

    public TokenStream(ErrorTable errorTable) {
        this.errorTable = errorTable;
    }

    private record CheckPoint(int pos, String description) {
    }

    // 添加Token到TokenStream中，仅同package的Lexer可访问
    void addToken(Token token) {
        this.list.add(token);
    }

    // 添加检查点
    public int checkpoint(String description) {
        this.checkpoints.add(new CheckPoint(pos, description + " at " + this.getNow()));
        return this.checkpoints.size() - 1;
    }

    // 恢复检查点
    public void restore(int checkpointID) {
        this.pos = this.checkpoints.get(checkpointID).pos();
    }

    // 距离检查点距离
    public int offset(int checkpointID) {
        return this.pos - this.checkpoints.get(checkpointID).pos();
    }

    // 是否还有Token
    public boolean hasNext() {
        return this.list.get(this.pos).type() != TokenType.EOF;
    }

    // 获取当前指向的Token
    public Token getNow() {
        return this.list.get(this.pos);
    }

    // 获取后offset指向的Token
    public Token getNext(int offset) {
        if (this.hasNext()) {
            return this.list.get(this.pos + offset);
        } else {
            return null;
        }
    }

    // 检查当前指向的Token是否是指定的类型
    public boolean isNow(TokenType... types) {
        Token nowToken = this.getNow();
        for (TokenType type : types) {
            if (nowToken.type() == type) {
                return true;
            }
        }
        return false;
    }

    // 检查后offset指向的Token是否是指定的类型
    public boolean isNext(int offset, TokenType... types) {
        Token aheadToken = this.getNext(offset);
        for (TokenType type : types) {
            if (aheadToken.type() == type) {
                return true;
            }
        }
        return false;
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

    // 如果当前指向的Token为types中指定的类型，则返回并且指针向后移动一位
    // 否则登记到错误处理表中
    public Token consumeOrError(String place, ErrorType errorType, TokenType... types) {
        if (types.length == 0 && Config.parserThrowable) {
            throw new RuntimeException("When " + place + ", none of type allowed");
        }
        Token ret = this.consumeOrNull(types);
        if (ret == null) {
            this.errorTable.addErrorRecord(this.getNext(-1).line(), errorType,
                    "When " + place + ", unexpected token: " + getNow()
                            + ". Expected: " + Arrays.toString(types));
        }
        return ret;
    }

    // 获取stream的ArrayList副本
    public ArrayList<Token> getArrayListCopy() {
        return new ArrayList<>(this.list);
    }
}
