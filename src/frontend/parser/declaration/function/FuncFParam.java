package frontend.parser.declaration.function;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.expression.ConstExp;
import frontend.type.ASTNode;
import frontend.type.TokenType;
import global.error.ErrorType;

import java.util.ArrayList;

public class FuncFParam implements ASTNode {
    public enum Type {
        BASIC,
        ARRAY,
    }

    private final Token typeToken;
    private final Token ident;
    private final Token lbrackToken;
    private final Token rbrackToken;

    // FuncFParam → BType Ident ['[' ']']
    FuncFParam(TokenStream stream) {
        String place = "FuncFParam()";
        // BType → 'int' | 'char'
        typeToken = stream.consumeOrThrow(place, TokenType.INTTK, TokenType.CHARTK);
        // Ident
        ident = stream.consumeOrThrow(place, TokenType.IDENFR);
        // ['[' ']']
        lbrackToken = stream.consumeOrNull(TokenType.LBRACK);
        rbrackToken = lbrackToken != null ? stream.consumeOrError(place, ErrorType.MISSING_RBRACK, TokenType.RBRACK) : null;
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(typeToken);
        ret.add(ident);
        if (getType() == Type.ARRAY) {
            ret.add(lbrackToken);
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

    public Token typeToken() {
        return typeToken;
    }
}
