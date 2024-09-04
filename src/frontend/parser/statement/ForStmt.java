package frontend.parser.statement;

import frontend.lexer.Token;
import frontend.lexer.TokenStream;
import frontend.parser.expression.Exp;
import frontend.parser.expression.LVal;
import frontend.type.ASTNode;
import frontend.type.TokenType;

import java.util.ArrayList;

public class ForStmt implements ASTNode {
    private final LVal lVal;
    private final Token assginToken;
    private final Exp exp;

    // ForStmt → LVal '=' Exp
    ForStmt(TokenStream stream) {
        String place = "ForStmt()";
        // LVal
        lVal = new LVal(stream);
        // '='
        assginToken = stream.consumeOrThrow(place, TokenType.ASSIGN);
        // Exp
        exp = new Exp(stream);
    }

    @Override
    public ArrayList<Object> explore() {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(lVal);
        ret.add(assginToken);
        ret.add(exp);
        return ret;
    }

    public LVal lVal() {
        return lVal;
    }

    public Exp exp() {
        return exp;
    }
}
