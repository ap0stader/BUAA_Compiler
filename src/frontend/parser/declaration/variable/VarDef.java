package frontend.parser.declaration.variable;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.expression.ConstExp;
import frontend.type.ASTNode;
import frontend.type.TokenType;
import global.error.ErrorType;

import java.util.ArrayList;

public class VarDef implements ASTNode {
    private final Token ident;
    private final Token lbrackToken;
    private final ConstExp constExp;
    private final Token rbrackToken;
    private final Token assignToken;
    private final InitVal initVal;

    // VarDef → Ident [ '[' ConstExp ']' ] [ '=' InitVal ]
    VarDef(TokenStream stream) {
        String place = "VarDef()";
        // Ident
        ident = stream.consumeOrThrow(place, TokenType.IDENFR);
        // [ '[' ConstExp ']' ]
        lbrackToken = stream.consumeOrNull(TokenType.LBRACK);
        constExp = lbrackToken != null ? new ConstExp(stream) : null;
        rbrackToken = lbrackToken != null ? stream.consumeOrError(place, ErrorType.MISSING_RBRACK, TokenType.RBRACK) : null;
        // [ '=' InitVal ]
        assignToken = stream.consumeOrNull(TokenType.ASSIGN);
        initVal = assignToken != null ? new InitVal(stream) : null;
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
        if (initVal != null) {
            ret.add(assignToken);
            ret.add(initVal);
        }
        return ret;
    }

    public Token ident() {
        return ident;
    }

    public ConstExp constExp() {
        return constExp;
    }

    public InitVal initVal() {
        return initVal;
    }
}
