package frontend.parser.declaration.constant;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.expression.ConstExp;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class ConstInitVal implements ASTNode {
    public enum Type {
        BASIC,
        ARRAY,
        STRING,
    }

    // BASIC
    private final ConstExp constExp;
    // ARRAY
    private final Token lbraceToken;
    private final ArrayList<ConstExp> constExps;
    private final ArrayList<Token> commaTokens;
    private final Token rbraceToken;
    // STRING
    private final Token stringConst;

    // ConstInitVal → ConstExp
    //              | '{' [ ConstExp { ',' ConstExp } ] '}'
    //              | StringConst
    ConstInitVal(TokenStream stream) {
        String place = "ConstInitVal()";
        if (stream.isNow(TokenType.LBRACE)) {
            // 该层为数组形式
            constExp = null;
            stringConst = null;
            // '{'
            lbraceToken = stream.consumeOrThrow(place, TokenType.LBRACE);
            // [ ConstExp { ',' ConstExp } ]
            constExps = new ArrayList<>();
            commaTokens = new ArrayList<>();
            if (stream.getNow().type() != TokenType.RBRACE) {
                // ConstExp
                constExps.add(new ConstExp(stream));
                // { ',' ConstExp }
                while (stream.isNow(TokenType.COMMA)) {
                    commaTokens.add(stream.consume());
                    constExps.add(new ConstExp(stream));
                }
            }
            // '}'
            rbraceToken = stream.consumeOrThrow(place, TokenType.RBRACE);
        } else if (stream.isNow(TokenType.STRCON)) {
            // 该层为字符串形式
            constExp = null;
            lbraceToken = null;
            constExps = null;
            commaTokens = null;
            rbraceToken = null;
            // StringConst
            stringConst = stream.consume();
        } else {
            // 该层为常量表达式
            lbraceToken = null;
            constExps = null;
            commaTokens = null;
            rbraceToken = null;
            stringConst = null;
            // ConstExp
            constExp = new ConstExp(stream);
        }
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        if (this.getType() == Type.BASIC) {
            ret.add(constExp);
        } else if (this.getType() == Type.STRING) {
            ret.add(stringConst);
        } else {
            ret.add(lbraceToken);
            if (!constExps.isEmpty()) {
                ret.add(constExps.get(0));
                for (int i = 1; i < constExps.size(); i++) {
                    ret.add(commaTokens.get(i - 1));
                    ret.add(constExps.get(i));
                }
            }
            ret.add(rbraceToken);
        }
        return ret;
    }

    public Type getType() {
        if (constExp != null) {
            return Type.BASIC;
        } else if (stringConst != null) {
            return Type.STRING;
        } else {
            return Type.ARRAY;
        }
    }

    public ConstExp constExp() {
        return constExp;
    }

    public ArrayList<ConstExp> constExps() {
        return constExps;
    }

    public Token stringConst() {
        return stringConst;
    }
}
