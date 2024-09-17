package frontend.parser.expression;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.type.ASTNode;
import frontend.type.TokenType;
import global.error.ErrorType;

import java.util.ArrayList;

public class LVal implements ASTNode {
    private final Token ident;
    private final ArrayList<Token> lbrackTokens;
    private final ArrayList<Exp> exps;
    private final ArrayList<Token> rbrackTokens;

    // LVal → Ident {'[' Exp ']'}
    public LVal(TokenStream stream) {
        String place = "LVal()";
        // Ident
        ident = stream.consumeOrThrow(place, TokenType.IDENFR);
        lbrackTokens = new ArrayList<>();
        exps = new ArrayList<>();
        rbrackTokens = new ArrayList<>();
        // {'[' Exp ']'}
        while (stream.isNow(TokenType.LBRACK)) {
            lbrackTokens.add(stream.consumeOrThrow(place, TokenType.LBRACK));
            exps.add(new Exp(stream));
            rbrackTokens.add(stream.consumeOrError(place, ErrorType.MISSING_RBRACK, TokenType.RBRACK));
        }
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(ident);
        for (int i = 0; i < exps.size(); i++) {
            ret.add(lbrackTokens.get(i));
            ret.add(exps.get(i));
            ret.add(rbrackTokens.get(i));
        }
        return ret;
    }

    public Token ident() {
        return ident;
    }

    public ArrayList<Exp> exps() {
        return exps;
    }
}
