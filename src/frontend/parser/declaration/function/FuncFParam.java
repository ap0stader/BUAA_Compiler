package frontend.parser.declaration.function;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.expression.ConstExp;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class FuncFParam implements ASTNode {
    public enum Type {
        BASIC,
        ARRAY,
    }

    private final Token typeToken;
    private final Token ident;
    private final ArrayList<Token> lbrackTokens;
    private final ArrayList<ConstExp> constExps;
    private final ArrayList<Token> rbrackTokens;

    // FuncFParam → 'int' Ident ['[' ']' { '[' ConstExp ']' }]
    FuncFParam(TokenStream stream) {
        String place = "FuncFParam()";
        // 'int'
        typeToken = stream.consumeOrThrow(place, TokenType.INTTK);
        // Indet
        ident = stream.consumeOrThrow(place, TokenType.IDENFR);
        // ['[' ']' { '[' ConstExp ']' }]
        if (stream.isNow(TokenType.LBRACK)) {
            lbrackTokens = new ArrayList<>();
            constExps = new ArrayList<>();
            rbrackTokens = new ArrayList<>();
            // '['
            lbrackTokens.add(stream.consumeOrThrow(place, TokenType.LBRACK));
            // ']'
            rbrackTokens.add(stream.consumeOrThrow(place, TokenType.RBRACK));
            // { '[' ConstExp ']' }
            while (stream.isNow(TokenType.LBRACK)) {
                lbrackTokens.add(stream.consumeOrThrow(place, TokenType.LBRACK));
                constExps.add(new ConstExp(stream));
                rbrackTokens.add(stream.consumeOrThrow(place, TokenType.RBRACK));
            }
        } else {
            lbrackTokens = null;
            constExps = null;
            rbrackTokens = null;
        }
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(typeToken);
        ret.add(ident);
        if (getType() == Type.ARRAY) {
            ret.add(lbrackTokens.get(0));
            ret.add(rbrackTokens.get(0));
            for (int i = 0; i < constExps.size(); i++) {
                ret.add(lbrackTokens.get(i + 1));
                ret.add(constExps.get(i));
                ret.add(rbrackTokens.get(i + 1));
            }
        }
        return ret;
    }

    public Type getType() {
        return lbrackTokens == null ? Type.BASIC : Type.ARRAY;
    }

    public Token ident() {
        return ident;
    }

    public ArrayList<ConstExp> constExps() {
        return constExps;
    }
}
