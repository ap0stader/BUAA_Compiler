package frontend.parser.declaration.function;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class FuncType implements ASTNode {
    private final Token typeToken;

    // FuncType → 'void' | 'int' | 'char'
    FuncType(TokenStream stream) {
        String place = "FuncType()";
        typeToken = stream.consumeOrThrow(place, TokenType.VOIDTK, TokenType.INTTK, TokenType.CHARTK);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(typeToken);
        return ret;
    }

    public Token typeToken() {
        return typeToken;
    }
}
