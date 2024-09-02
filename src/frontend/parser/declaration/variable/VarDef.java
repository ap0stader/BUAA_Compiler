package frontend.parser.declaration.variable;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.expression.ConstExp;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class VarDef implements ASTNode {
    private final Token ident;
    private final ArrayList<Token> lbrackTokens;
    private final ArrayList<ConstExp> constExps;
    private final ArrayList<Token> rbrackTokens;
    private final Token eqlToken;
    private final InitVal initVal;

    // VarDef → Ident { '[' ConstExp ']' }
    //        | Ident { '[' ConstExp ']' } '=' InitVal
    public VarDef(TokenStream stream) {
        String place = "VarDef()";
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
        if (stream.getNow().type() == TokenType.EQL) {
            // '='
            eqlToken = stream.consumeOrThrow(place, TokenType.EQL);
            // InitVal
            initVal = new InitVal(stream);
        } else {
            eqlToken = null;
            initVal = null;
        }
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
        if (eqlToken != null) {
            ret.add(eqlToken);
            ret.add(initVal);
        }
        return ret;
    }

    public Token ident() {
        return ident;
    }

    public ArrayList<ConstExp> constExps() {
        return constExps;
    }

    public InitVal initVal() {
        return initVal;
    }

    public Token eqlToken() {
        return eqlToken;
    }
}
