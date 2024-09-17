package frontend.parser.expression;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class Character implements ASTNode {
    private final Token charConst;

    // Character → CharConst
    Character(TokenStream stream) {
        String place = "Character()";
        charConst = stream.consumeOrThrow(place, TokenType.CHRCON);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(charConst);
        return ret;
    }

    public Token charConst() {
        return charConst;
    }
}
