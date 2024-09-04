package frontend.parser.expression;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class Number implements ASTNode {
    private final Token intConst;

    Number(TokenStream stream) {
        String place = "Number()";
        intConst = stream.consumeOrThrow(place, TokenType.INTCON);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(intConst);
        return ret;
    }

    public Token intConst() {
        return intConst;
    }
}
