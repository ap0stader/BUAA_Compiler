package frontend.parser.declaration.constant;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.expression.ConstExp;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class ConstInitVal implements ASTNode {
    public enum Type {
        SINGLE,
        MULTIPLE,
    }

    private final ConstExp constExp;
    private final Token lbraceToken;
    private final ArrayList<ConstInitVal> constInitVals;
    private final ArrayList<Token> commaTokens;
    private final Token rbraceToken;

    // ConstInitVal → ConstExp
    //              | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    public ConstInitVal(TokenStream stream) {
        String place = "ConstInitVal()";
        if (stream.isNow(TokenType.LBRACE)) {
            // 该层为数组形式
            constExp = null;
            // '{'
            lbraceToken = stream.consumeOrThrow(place, TokenType.LBRACE);
            // [ ConstInitVal { ',' ConstInitVal } ]
            constInitVals = new ArrayList<>();
            commaTokens = new ArrayList<>();
            if (stream.isNow(TokenType.RBRACE)) {
                // ConstInitVal
                constInitVals.add(new ConstInitVal(stream));
                // { ',' ConstInitVal }
                while (stream.isNow(TokenType.COMMA)) {
                    commaTokens.add(stream.consume());
                    constInitVals.add(new ConstInitVal(stream));
                }
            }
            // '}'
            rbraceToken = stream.consumeOrThrow(place, TokenType.RBRACE);
        } else {
            // 该层为常量表达式
            // ConstExp
            constExp = new ConstExp(stream);
            lbraceToken = null;
            constInitVals = null;
            commaTokens = null;
            rbraceToken = null;
        }
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        if (this.getType() == Type.SINGLE) {
            ret.add(constExp);
        } else {
            ret.add(lbraceToken);
            if (!constInitVals.isEmpty()) {
                ret.add(constInitVals.get(0));
                for (int i = 1; i < constInitVals.size(); i++) {
                    ret.add(commaTokens.get(i - 1));
                    ret.add(constInitVals.get(i));
                }
            }
            ret.add(rbraceToken);
        }
        return ret;
    }

    public Type getType() {
        return constExp == null ? Type.MULTIPLE : Type.SINGLE;
    }

    public ConstExp constExp() {
        return constExp;
    }

    public ArrayList<ConstInitVal> constInitVals() {
        return constInitVals;
    }
}
