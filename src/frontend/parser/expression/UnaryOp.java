package frontend.parser.expression;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class UnaryOp implements ASTNode {
    private final Token symbol;

    // UnaryOp → '+' | '−' | '!'
    UnaryOp(TokenStream stream) {
        String place = "UnaryOp()";
        symbol = stream.consumeOrThrow(place, TokenType.PLUS, TokenType.MINU, TokenType.NOT);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(symbol);
        return ret;
    }

    public Token symbol() {
        return symbol;
    }
}
