package frontend.parser.declaration.constant;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.expression.ConstExp;
import frontend.type.ASTNode;
import frontend.type.TokenType;
import global.error.ErrorType;

import java.util.ArrayList;

public class ConstDef implements ASTNode {
    private final Token ident;
    private final Token lbrackToken;
    private final ConstExp constExp;
    private final Token rbrackToken;
    private final Token assignToken;
    private final ConstInitVal constInitVal;

    // ConstDef → Ident [ '[' ConstExp ']' ] '=' ConstInitVal
    ConstDef(TokenStream stream) {
        String place = "ConstDef()";
        // Ident
        ident = stream.consumeOrThrow(place, TokenType.IDENFR);
        // [ '[' ConstExp ']' ]
        lbrackToken = stream.consumeOrNull(TokenType.LBRACK);
        constExp = lbrackToken != null ? new ConstExp(stream) : null;
        rbrackToken = lbrackToken != null ? stream.consumeOrError(place, ErrorType.MISSING_RBRACK, TokenType.RBRACK) : null;
        // '='
        assignToken = stream.consumeOrThrow(place, TokenType.ASSIGN);
        // ConstInitVal
        constInitVal = new ConstInitVal(stream);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(ident);
        if (constExp != null) {
            ret.add(lbrackToken);
            ret.add(constExp);
            ret.add(rbrackToken);
        }
        ret.add(assignToken);
        ret.add(constInitVal);
        return ret;
    }

    public Token ident() {
        return ident;
    }

    public ConstExp constExp() {
        return constExp;
    }

    public ConstInitVal constInitVal() {
        return constInitVal;
    }
}
