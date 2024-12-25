package frontend.parser.expression;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;
import frontend.type.ErrorType;

import java.util.ArrayList;

public class LVal implements ASTNode {
    public enum Type {
        BASIC,
        ARRAY,
    }

    private final Token ident;
    private final Token lbrackToken;
    private final Exp exp;
    private final Token rbrackToken;

    // LVal → Ident ['[' Exp ']']
    public LVal(TokenStream stream) {
        String place = "LVal()";
        // Ident
        ident = stream.consumeOrThrow(place, TokenType.IDENFR);
        // ['[' Exp ']']
        lbrackToken = stream.consumeOrNull(TokenType.LBRACK);
        exp = lbrackToken != null ? new Exp(stream) : null;
        rbrackToken = lbrackToken != null ? stream.consumeOrError(place, ErrorType.MISSING_RBRACK, TokenType.RBRACK) : null;
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(ident);
        if (exp != null) {
            ret.add(lbrackToken);
            ret.add(exp);
            ret.add(rbrackToken);
        }
        return ret;
    }

    public Type getType() {
        return lbrackToken == null ? Type.BASIC : Type.ARRAY;
    }

    public Token ident() {
        return ident;
    }

    public Exp exp() {
        return exp;
    }
}
