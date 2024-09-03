package frontend.parser.declaration.variable;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.expression.Exp;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class InitVal implements ASTNode {
    public enum Type {
        BASIC,
        ARRAY,
    }

    private final Exp exp;
    private final Token lbraceToken;
    private final ArrayList<InitVal> initVals;
    private final ArrayList<Token> commaTokens;
    private final Token rbraceToken;

    // InitVal → Exp
    //         | '{' [ InitVal { ',' InitVal } ] '}'
    public InitVal(TokenStream stream) {
        String place = "InitVal()";
        if (stream.isNow(TokenType.LBRACE)) {
            // 该层为数组形式
            exp = null;
            // '{'
            lbraceToken = stream.consumeOrThrow(place, TokenType.LBRACE);
            // [ InitVal { ',' InitVal } ]
            initVals = new ArrayList<>();
            commaTokens = new ArrayList<>();
            if (stream.isNow(TokenType.RBRACE)) {
                // InitVal
                initVals.add(new InitVal(stream));
                // { ',' InitVal }
                while (stream.isNow(TokenType.COMMA)) {
                    commaTokens.add(stream.consume());
                    initVals.add(new InitVal(stream));
                }
            }
            // '}'
            rbraceToken = stream.consumeOrThrow(place, TokenType.RBRACE);
        } else {
            // 该层为表达式
            // Exp
            exp = new Exp(stream);
            lbraceToken = null;
            initVals = null;
            commaTokens = null;
            rbraceToken = null;
        }
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        if (this.getType() == Type.BASIC) {
            ret.add(exp);
        } else {
            ret.add(lbraceToken);
            if (!initVals.isEmpty()) {
                ret.add(initVals.get(0));
                for (int i = 1; i < initVals.size(); i++) {
                    ret.add(commaTokens.get(i - 1));
                    ret.add(initVals.get(i));
                }
            }
            ret.add(rbraceToken);
        }
        return ret;
    }

    public Type getType() {
        return exp != null ? Type.BASIC : Type.ARRAY;
    }

    public Exp exp() {
        return exp;
    }

    public ArrayList<InitVal> initVals() {
        return initVals;
    }
}
