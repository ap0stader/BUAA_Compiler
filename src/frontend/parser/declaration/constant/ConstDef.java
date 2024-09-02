package frontend.parser.declaration.constant;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.expression.ConstExp;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class ConstDef implements ASTNode {
    private final Token ident;
    private final ArrayList<Token> lbrackTokens;
    private final ArrayList<ConstExp> constExps;
    private final ArrayList<Token> rbrackTokens;
    private final Token eqlToken;
    private final ConstInitVal constInitVal;

    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    public ConstDef(TokenStream stream) {
        String place = "ConstDef()";
        // Ident
        ident = stream.consumeOrThrow(place, TokenType.IDENFR);
        // { '[' ConstExp ']' }
        lbrackTokens = new ArrayList<>();
        constExps = new ArrayList<>();
        rbrackTokens = new ArrayList<>();
        while (stream.getNow().type() == TokenType.LBRACK) {
            lbrackTokens.add(stream.consumeOrThrow(place, TokenType.LBRACK));
            constExps.add(new ConstExp(stream));
            rbrackTokens.add(stream.consumeOrThrow(place, TokenType.RBRACK));
        }
        // '='
        eqlToken = stream.consumeOrThrow(place, TokenType.EQL);
        // ConstInitVal
        constInitVal = new ConstInitVal(stream);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(ident);
        for (int i = 0; i < constExps.size(); i++) {
            ret.add(lbrackTokens.get(i));
            ret.add(constExps.get(i));
            ret.add(rbrackTokens.get(i));
        }
        ret.add(eqlToken);
        ret.add(constInitVal);
        return ret;
    }

    public Token ident() {
        return ident;
    }

    public ArrayList<ConstExp> constExps() {
        return constExps;
    }

    public ConstInitVal constInitVal() {
        return constInitVal;
    }
}
