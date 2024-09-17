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
        STRING,
    }

    // BASIC
    private final Exp exp;
    // ARRAY
    private final Token lbraceToken;
    private final ArrayList<Exp> exps;
    private final ArrayList<Token> commaTokens;
    private final Token rbraceToken;
    // STRING
    private final Token stringConst;

    // InitVal → Exp
    //         | '{' [ Exp { ',' Exp } ] '}'
    //         | StringConst
    InitVal(TokenStream stream) {
        String place = "InitVal()";
        if (stream.isNow(TokenType.LBRACE)) {
            // 该层为数组形式
            exp = null;
            stringConst = null;
            // '{'
            lbraceToken = stream.consumeOrThrow(place, TokenType.LBRACE);
            // [ Exp { ',' Exp } ]
            exps = new ArrayList<>();
            commaTokens = new ArrayList<>();
            if (stream.getNow().type() != TokenType.RBRACE) {
                // Exp
                exps.add(new Exp(stream));
                // { ',' Exp }
                while (stream.isNow(TokenType.COMMA)) {
                    commaTokens.add(stream.consume());
                    exps.add(new Exp(stream));
                }
            }
            // '}'
            rbraceToken = stream.consumeOrThrow(place, TokenType.RBRACE);
        } else if (stream.isNow(TokenType.STRCON)) {
            // 该层为字符串形式
            exp = null;
            lbraceToken = null;
            exps = null;
            commaTokens = null;
            rbraceToken = null;
            // StringConst
            stringConst = stream.consume();
        } else {
            // 该层为表达式
            lbraceToken = null;
            exps = null;
            commaTokens = null;
            rbraceToken = null;
            stringConst = null;
            // Exp
            exp = new Exp(stream);
        }
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        if (this.getType() == Type.BASIC) {
            ret.add(exp);
        } else if (this.getType() == Type.STRING) {
            ret.add(stringConst);
        } else {
            ret.add(lbraceToken);
            if (!exps.isEmpty()) {
                ret.add(exps.get(0));
                for (int i = 1; i < exps.size(); i++) {
                    ret.add(commaTokens.get(i - 1));
                    ret.add(exps.get(i));
                }
            }
            ret.add(rbraceToken);
        }
        return ret;
    }

    public Type getType() {
        if (exp != null) {
            return Type.BASIC;
        } else if (stringConst != null) {
            return Type.STRING;
        } else {
            return Type.ARRAY;
        }
    }

    public Exp exp() {
        return exp;
    }

    public ArrayList<Exp> exps() {
        return exps;
    }

    public Token stringConst() {
        return stringConst;
    }
}
